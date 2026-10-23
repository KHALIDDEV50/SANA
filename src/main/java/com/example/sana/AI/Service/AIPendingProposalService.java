package com.example.sana.AI.Service;

import com.example.sana.AI.DTO.*;
import com.example.sana.AI.Enums.AIReplyType;
import com.example.sana.API.ApiException;

import com.example.sana.Enums.ChangeOperation;
import com.example.sana.Enums.ChangeTargetType;
import com.example.sana.Enums.ProposalStatus;

import com.example.sana.Model.*;

import com.example.sana.Repository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AIPendingProposalService {

    private final AIConversationService conversationService;
    private final AIProposalPreparationService proposalPreparationService;
    private final AICharacterUpdateService aiCharacterUpdateService;
    private final AIWorldUpdateService aiWorldUpdateService;
    private final AILocationProposalService aiLocationProposalService;
    private final AIWorldRuleProposalService aiWorldRuleProposalService;
    private final AIStoryEventProposalService aiStoryEventProposalService;

    private final ProjectRepository projectRepository;
    private final AIInteractionRepository aiInteractionRepository;
    private final ChangeProposalRepository changeProposalRepository;
    private final ChangeProposalItemRepository changeProposalItemRepository;

    private final ProjectNodeRepository projectNodeRepository;
    private final ProjectEdgeRepository projectEdgeRepository;
    private final ProjectRecordRepository projectRecordRepository;

    private final JsonMapper jsonMapper;


    // Save the AI response and its pending proposal atomically
    @Transactional
    public SanaAIResponseDTO savePendingProposal(
            Integer projectId,
            Integer authenticatedUserId,
            Integer parentInteractionId,
            SanaAIPreparedRequestDTO preparedRequest,
            String assistantMessage,
            String actualModelName,
            AIProposalDraftDTO draft) {

        // Verify authorization before reading or saving data
        Project project = getAuthorizedProject(
                projectId, authenticatedUserId);

        if (preparedRequest == null
                || !projectId.equals(preparedRequest.projectId())) {
            throw new ApiException("Invalid prepared AI request");
        }

        // Validate the AI-generated draft
        AIProposalDraftDTO validatedDraft =
                proposalPreparationService.prepareDraft(
                        projectId, draft);

        // Save this turn as a completed PROPOSAL response
        SanaAIResponseDTO response =
                conversationService.saveCompletedTurn(
                        projectId,
                        authenticatedUserId,
                        parentInteractionId,
                        preparedRequest,
                        AIReplyType.PROPOSAL,
                        assistantMessage,
                        null,
                        actualModelName
                );

        AIInteraction interaction =
                aiInteractionRepository.findById(
                                response.interactionId())
                        .orElseThrow(() ->
                                new ApiException("AI interaction not found"));

        // Create a proposal for user review
        ChangeProposal proposal = new ChangeProposal();

        proposal.setTitle(validatedDraft.title());
        proposal.setDescription(validatedDraft.description());
        proposal.setStatus(ProposalStatus.PENDING);
        proposal.setBaseVersion(project.getVersion());
        proposal.setProject(project);
        proposal.setAiInteraction(interaction);

        ChangeProposal savedProposal =
                changeProposalRepository.save(proposal);

        // Save all proposed changes without applying them
        for (AIProposalDraftDTO.ProposedItem draftItem
                : validatedDraft.items()) {

            ChangeProposalItem item = new ChangeProposalItem();

            item.setOperation(draftItem.operation());
            item.setTargetType(draftItem.targetType());
            item.setTargetId(draftItem.targetId());

            // Snapshot the original target for review
            if (draftItem.operation() == ChangeOperation.CREATE) {
                item.setBeforeJson(null);
            } else {
                // Include the project ID so specialized snapshot handlers
                // can verify that the target belongs to this project.
                item.setBeforeJson(createBeforeSnapshot(projectId, draftItem));

            }

            // Store the suggested data and its metadata
            if (draftItem.operation() == ChangeOperation.DELETE) {
                item.setAfterJson(null);
            } else {
                item.setAfterJson(
                        createAfterSnapshot(draftItem));
            }

            item.setChangeProposal(savedProposal);
            changeProposalItemRepository.save(item);
        }

        // Return the saved proposal ID to the frontend
        return new SanaAIResponseDTO(
                response.interactionId(),
                response.mode(),
                response.task(),
                AIReplyType.PROPOSAL,
                response.message(),
                savedProposal.getId(),
                null
        );
    }


    // Retrieve a saved proposal for review
    @Transactional(readOnly = true)
    public AIPendingProposalDTO getPendingProposal(
            Integer projectId,
            Integer authenticatedUserId,
            Integer proposalId) {

        getAuthorizedProject(projectId, authenticatedUserId);

        ChangeProposal proposal =
                changeProposalRepository.findById(proposalId)
                        .orElseThrow(() ->
                                new ApiException("Proposal not found"));

        if (!projectId.equals(proposal.getProject().getId())) {
            throw new ApiException(
                    "Proposal does not belong to this project");
        }

        AIInteraction interaction = proposal.getAiInteraction();

        // AI proposals must belong to the same authenticated user
        if (interaction == null
                || !authenticatedUserId.equals(
                interaction.getUser().getId())) {

            throw new ApiException("Access denied to this proposal");
        }

        List<ChangeProposalItem> items =
                changeProposalItemRepository
                        .findByChangeProposalId(proposalId);

        List<AIPendingProposalDTO.ItemInfo> itemInfo =
                items.stream()
                        .sorted(Comparator.comparing(
                                ChangeProposalItem::getId))
                        .map(item ->
                                new AIPendingProposalDTO.ItemInfo(
                                        item.getId(),
                                        item.getOperation(),
                                        item.getTargetType(),
                                        item.getTargetId(),
                                        item.getBeforeJson(),
                                        item.getAfterJson()
                                )
                        )
                        .toList();

        return new AIPendingProposalDTO(
                proposal.getId(),
                projectId,
                interaction.getId(),
                proposal.getTitle(),
                proposal.getDescription(),
                proposal.getStatus(),
                proposal.getBaseVersion(),
                itemInfo
        );
    }


    // Capture original values for UPDATE and DELETE review
    private String createBeforeSnapshot(Integer projectId, AIProposalDraftDTO.ProposedItem item) {

        Map<String, Object> data = new LinkedHashMap<>();

        Integer targetId = item.targetId();

        switch (item.targetType()) {



            case NODE -> {

                // Character updates need a full CharacterProfile snapshot.
                // A generic ProjectNode snapshot would not include
                // the character's goals, history, traits, etc.
                if (item.operation() == ChangeOperation.UPDATE && "CHARACTER".equals(item.elementType())) {

                    return aiCharacterUpdateService.createBeforeSnapshot(projectId, item.targetId());
                }

                // World updates also need a specialized snapshot.
                // It includes history, lore, atmosphere, notes
                // and the linked ProjectNode information.
                if (item.operation() == ChangeOperation.UPDATE && "WORLD".equals(item.elementType())) {

                    return aiWorldUpdateService.createBeforeSnapshot(projectId, item.targetId());
                }

                // Location updates must capture the complete Location
                // and its world/parent relationships.
                if (item.operation() == ChangeOperation.UPDATE && "LOCATION".equals(item.elementType())) {

                    return aiLocationProposalService.createBeforeSnapshot(projectId, item.targetId());
                }
                // WorldRule updates need a specialized snapshot.
                // The snapshot includes rule details and its World relationship.
                if (item.operation() == ChangeOperation.UPDATE && "WORLD_RULE".equals(item.elementType())) {

                    return aiWorldRuleProposalService.createBeforeSnapshot(projectId, item.targetId());
                }

                // StoryEvent updates require a full snapshot.
                // Include event details and both narrative ordering fields,
                // not only the underlying ProjectNode information.
                if (item.operation() == ChangeOperation.UPDATE && "STORY_EVENT".equals(item.elementType())) {

                    return aiStoryEventProposalService.createBeforeSnapshot(projectId, item.targetId());
                }



                // Preserve the existing behavior for generic node updates.
                // Only basic ProjectNode fields are included here.
                ProjectNode node = projectNodeRepository.findById(item.targetId()).orElseThrow(() -> new ApiException("Node not found"));

                data.put("type", node.getType());
                data.put("dataJson", node.getDataJson());
                data.put("positionX", node.getPositionX());
                data.put("positionY", node.getPositionY());
                data.put("outputId", node.getProjectOutput().getId());
            }


            case EDGE -> {
                ProjectEdge edge = projectEdgeRepository.findById(targetId).orElseThrow(() -> new ApiException("Edge not found"));

                data.put("type", edge.getType());
                data.put("label", edge.getLabel());
                data.put("dataJson", edge.getDataJson());
                data.put("sourceNodeId", edge.getSourceNode().getId());
                data.put("targetNodeId", edge.getTargetNode().getId());
                data.put("outputId", edge.getProjectOutput().getId());
            }

            case RECORD -> {
                ProjectRecord record = projectRecordRepository.findById(targetId).orElseThrow(() -> new ApiException("Record not found"));

                data.put("title", record.getTitle());
                data.put("content", record.getContent());
                data.put("type", record.getType());
                data.put("status", record.getStatus());
                data.put("source", record.getSource());
                data.put("version", record.getVersion());
            }
        }

        return toJson(data);
    }


    // Preserve the output, element type and proposed fields
    private String createAfterSnapshot(AIProposalDraftDTO.ProposedItem item) {

        Map<String, Object> data = new LinkedHashMap<>();

        data.put("outputId", item.outputId());
        data.put("elementType", item.elementType());
        data.put("fields", item.proposedFields());

        return toJson(data);
    }


    // Serialize plain data, not JPA entities
    private String toJson(Object data) {

        try {
            return jsonMapper.writeValueAsString(data);
        } catch (JacksonException e) {
            throw new ApiException("Failed to serialize AI proposal data");
        }
    }


    // Ensure only the project owner can use this workflow
    private Project getAuthorizedProject(Integer projectId, Integer authenticatedUserId) {

        if (projectId == null || authenticatedUserId == null) {
            throw new ApiException("Project and authenticated user are required");
        }

        Project project = projectRepository.findById(projectId).orElseThrow(() -> new ApiException("Project not found"));

        if (project.getOwner() == null || !authenticatedUserId.equals(project.getOwner().getId())) {

            throw new ApiException("Access denied to this project");
        }

        return project;
    }
}
