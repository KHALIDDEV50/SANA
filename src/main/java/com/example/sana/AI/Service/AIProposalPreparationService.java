package com.example.sana.AI.Service;

import com.example.sana.AI.DTO.AIProposalDraftDTO;
import com.example.sana.API.ApiException;

import com.example.sana.Enums.ChangeOperation;
import com.example.sana.Enums.ChangeTargetType;

import com.example.sana.Model.Project;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Model.ProjectEdge;
import com.example.sana.Model.ProjectRecord;

import com.example.sana.Repository.ProjectRepository;
import com.example.sana.Repository.ProjectOutputRepository;
import com.example.sana.Repository.ProjectNodeRepository;
import com.example.sana.Repository.ProjectEdgeRepository;
import com.example.sana.Repository.ProjectRecordRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import com.example.sana.AI.Enums.*;

@Service
@RequiredArgsConstructor
public class AIProposalPreparationService {

    private final ProjectRepository projectRepository;
    private final ProjectOutputRepository projectOutputRepository;
    private final ProjectNodeRepository projectNodeRepository;
    private final ProjectEdgeRepository projectEdgeRepository;
    private final ProjectRecordRepository projectRecordRepository;
    private final AIGameProposalService aiGameProposalService;

    private static final Set<String> NODE_TYPES = Set.of(
            "CHARACTER",
            "WORLD",
            "LOCATION",
            "WORLD_RULE",
            "STORY_EVENT"
    );


    // Validate a proposal draft before presenting it for review
    // This method does not apply or save any proposed changes
    @Transactional(readOnly = true)
    public AIProposalDraftDTO prepareDraft(Integer projectId, AIProposalDraftDTO draft) {
        Project project = projectRepository.findProjectById(projectId);

        if (project == null) {
            throw new ApiException("Project not found");
        }

        if (draft == null || draft.title() == null || draft.title().isBlank() || draft.title().length() > 255 || draft.description() == null || draft.description().isBlank()) {
            throw new ApiException("Invalid AI proposal details");
        }

        if (draft.items() == null || draft.items().isEmpty() || draft.items().size() > 20) {

            throw new ApiException("An AI proposal must contain between 1 and 20 items");
        }

        for (AIProposalDraftDTO.ProposedItem item : draft.items()) {
            validateItem(projectId, item);
        }

        return draft;
    }


    // Validate operation, target type and project ownership
    private void validateItem(Integer projectId, AIProposalDraftDTO.ProposedItem item) {

        if (item == null || item.operation() == null || item.targetType() == null) throw new ApiException("Invalid AI proposal item");

        // Reject unsupported operations before creating a PENDING proposal.
        if (item.operation() == ChangeOperation.DELETE) throw new ApiException("AI DELETE is not supported yet");

        // Game elements use their own target mappings and repository checks.
        if (aiGameProposalService.supports(item.elementType())) {
            aiGameProposalService.validateDraft(projectId, item);
            return;
        }

        if (item.operation() == ChangeOperation.CREATE) {

            if (item.targetId() != null) {
                throw new ApiException("CREATE operation must not specify a target ID");
            }

            validateCreateTarget(projectId, item);

        } else {

            if (item.targetId() == null || item.targetId() <= 0) {

                throw new ApiException("UPDATE and DELETE require a valid target ID");
            }

            validateExistingTarget(projectId, item);
        }

        // CREATE and UPDATE must contain proposed data
        if (item.operation() != ChangeOperation.DELETE) {

            if (item.proposedFields() == null || item.proposedFields().isEmpty()) {

                throw new ApiException("Proposed fields are required");
            }
        }

        // Never allow the AI to assign protected fields directly
        if (item.proposedFields() != null) {

            Set<String> protectedFields = Set.of(
                    "id",
                    "owner",
                    "ownerId",
                    "userId",
                    "projectId",
                    "projectOutput",
                    "createdAt",
                    "updatedAt"
            );

            for (String field : item.proposedFields().keySet()) {

                if (protectedFields.contains(field)) {
                    throw new ApiException("AI cannot modify protected field: " + field);
                }
            }
        }
    }


    // Validate the output and element type for new elements
    private void validateCreateTarget(Integer projectId, AIProposalDraftDTO.ProposedItem item) {

        // Generic record and edge creation do not have AI approval handlers.
        if (item.targetType() != ChangeTargetType.NODE || !NODE_TYPES.contains(item.elementType())) {
            throw new ApiException("Unsupported AI CREATE target");
        }

        if (item.outputId() == null) {
            throw new ApiException("Output ID is required for creating nodes or edges");
        }

        ProjectOutput output = projectOutputRepository.findProjectOutputById(item.outputId());

        if (output == null || !output.getProject().getId().equals(projectId)) {

            throw new ApiException("Output does not belong to this project");
        }

        if (item.targetType() == ChangeTargetType.NODE && (item.elementType() == null || !NODE_TYPES.contains(item.elementType()))) {

            throw new ApiException("Invalid creative node type");
        }
    }


    // Make sure existing targets belong to the same project
    private void validateExistingTarget(Integer projectId, AIProposalDraftDTO.ProposedItem item) {

        Integer targetId = item.targetId();
        Integer targetProjectId;
        Integer targetOutputId = null;

        switch (item.targetType()) {

            case NODE -> {
                ProjectNode node = projectNodeRepository.findById(targetId).orElseThrow(() -> new ApiException("Target node not found"));

                targetOutputId = node.getProjectOutput().getId();
                targetProjectId = node.getProjectOutput().getProject().getId();
            }

            case EDGE -> {
                ProjectEdge edge = projectEdgeRepository.findById(targetId).orElseThrow(() -> new ApiException("Target edge not found"));

                targetOutputId = edge.getProjectOutput().getId();
                targetProjectId = edge.getProjectOutput().getProject().getId();
            }

            case RECORD -> {
                ProjectRecord record = projectRecordRepository.findById(targetId).orElseThrow(() -> new ApiException("Target record not found"));

                targetProjectId = record.getProject().getId();
            }

            default -> throw new ApiException("Unsupported target type");
        }

        if (!projectId.equals(targetProjectId)) {
            throw new ApiException("Proposed target belongs to another project");
        }

        if (item.outputId() != null && !item.outputId().equals(targetOutputId)) throw new ApiException("Proposal output does not match the target");

        // A creative entity must use its exact specialized handler.
        if (item.targetType() == ChangeTargetType.NODE) {
            if (item.elementType() == null) throw new ApiException("AI elementType is required");
            ProjectNode node = projectNodeRepository.findById(targetId).orElseThrow(() -> new ApiException("Node not found"));
            if (!item.elementType().equals(node.getType())) throw new ApiException("AI elementType does not match the actual node");
        }
        if (item.targetType() == ChangeTargetType.EDGE && !"EDGE".equals(item.elementType())) throw new ApiException("Only generic EDGE metadata updates are allowed here");
        if (item.targetType() == ChangeTargetType.RECORD && !"RECORD".equals(item.elementType())) throw new ApiException("Only ProjectRecord updates are allowed here");
    }
}
