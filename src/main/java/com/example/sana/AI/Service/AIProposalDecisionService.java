package com.example.sana.AI.Service;

import com.example.sana.API.ApiException;
import com.example.sana.Enums.*;
import com.example.sana.Model.*;

import com.example.sana.Repository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AIProposalDecisionService {


    private final AICharacterCreationService aiCharacterCreationService;
    private final AICharacterUpdateService aiCharacterUpdateService;
    private final AIWorldCreationService aiWorldCreationService;
    private final AIWorldUpdateService aiWorldUpdateService;
    private final AILocationProposalService aiLocationProposalService;
    private final AIWorldRuleProposalService aiWorldRuleProposalService;
    private final AIStoryEventProposalService aiStoryEventProposalService;
    private final AIGameProposalService aiGameProposalService;
    private final ChangeProposalRepository changeProposalRepository;
    private final ChangeProposalItemRepository changeProposalItemRepository;
    private final ProjectRepository projectRepository;
    private final ProjectNodeRepository projectNodeRepository;
    private final ProjectEdgeRepository projectEdgeRepository;
    private final ProjectRecordRepository projectRecordRepository;
    private final JsonMapper jsonMapper;

    // These fields can be updated through an AI proposal
    private static final Set<String> NODE_FIELDS =
            Set.of("dataJson", "positionX", "positionY");

    private static final Set<String> EDGE_FIELDS =
            Set.of("label", "dataJson");

    private static final Set<String> RECORD_FIELDS =
            Set.of("title", "content", "status", "source");


    // Reject a pending AI proposal without changing project data

    @Transactional
    public void rejectProposal(Integer projectId, Integer authenticatedUserId, Integer proposalId) {

        ChangeProposal proposal = loadAuthorizedProposal(projectId, authenticatedUserId, proposalId);

        requirePending(proposal);

        proposal.setStatus(ProposalStatus.REJECTED);
        proposal.setReviewedAt(LocalDateTime.now());

        changeProposalRepository.save(proposal);
    }


    // Approve and apply an AI-generated proposal.
    // All changes must be completed successfully before the
    // proposal can be marked as APPROVED.
    @Transactional
    public void approveProposal(Integer projectId, Integer authenticatedUserId, Integer proposalId) {

        // STEP 1: Load and lock the proposal.
        // This method also verifies that the authenticated user
        // is authorized to access the proposal and its project.
        ChangeProposal proposal = loadAuthorizedProposal(projectId, authenticatedUserId, proposalId);

        // Only proposals waiting for review can be approved.
        // This prevents approving the same proposal twice.
        requirePending(proposal);


        // STEP 2: Lock the project during the approval process.
        // This helps coordinate concurrent proposal approvals.
        Project project = projectRepository.findByIdForUpdate(projectId).orElseThrow(() -> new ApiException("Project not found"));


        // STEP 3: Validate the project version.
        // Reject outdated proposals if the project version
        // has changed since the proposal was created.
        if (!Objects.equals(proposal.getBaseVersion(), project.getVersion())) {

            throw new ApiException("Project has changed. Please review the proposal again");
        }


        // STEP 4: Retrieve all changes included in the proposal.
        // Use the existing repository instead of EntityManager.
        List<ChangeProposalItem> items = new ArrayList<>(changeProposalItemRepository.findByChangeProposalId(proposalId));

        // Apply the items in a consistent order.
        items.sort(Comparator.comparing(ChangeProposalItem::getId));

        // A proposal must contain at least one change.
        if (items.isEmpty()) {
            throw new ApiException("Proposal has no changes");
        }


        // STEP 5: Validate each saved item, including game snapshots and creative DTOs.
        for (ChangeProposalItem item : items) validateSavedItem(projectId, item);


        // STEP 6: Apply the approved changes.
        // All operations run inside the same transaction.
        // If any operation fails, the transaction rolls back.
        for (ChangeProposalItem item : items) {

            switch (item.getOperation()) {

                // Apply the approved creation using its specialized handler.
                case CREATE:
                    applyCreate(projectId, item);
                    break;

                // Apply an update.
                // The applyUpdate() method routes character changes
                // to AICharacterUpdateService and handles supported
                // general NODE, EDGE, and RECORD updates.
                case UPDATE:
                    applyUpdate(projectId, item);
                    break;


                // Safety check: never execute unsupported deletion.
                case DELETE:
                    throw new ApiException("DELETE is not supported yet");
                default:
                    throw new ApiException("Unsupported proposal operation");
            }
        }


        // STEP 7: Mark the project as modified.
        // The Project entity has an @Version field.
        // Hibernate manages its increment when the project is updated.
        project.setUpdatedAt(LocalDateTime.now());
        projectRepository.save(project);


        // STEP 8: Mark the proposal as approved.
        // This happens only after all changes pass the
        // validation and application phases.
        proposal.setStatus(ProposalStatus.APPROVED);
        proposal.setReviewedAt(LocalDateTime.now());

        changeProposalRepository.save(proposal);


        // STEP 9: Flush pending SQL statements.
        // The transaction still commits after this method finishes.
        // Any failure during the transaction causes a rollback.
        changeProposalRepository.flush();
    }




    // Shared validation used both when a proposal is saved and approved.
    // Game proposals verify their own target IDs, fields and full snapshots.
    public void validateSavedItem(Integer projectId, ChangeProposalItem item) {
        if (item == null || item.getOperation() == null || item.getTargetType() == null) throw new ApiException("Invalid AI proposal item");
        if (item.getOperation() == ChangeOperation.DELETE) throw new ApiException("AI DELETE is not supported");
        Map<String, Object> after = readJsonMap(item.getAfterJson());
        if (!after.keySet().equals(Set.of("outputId", "elementType", "fields"))) throw new ApiException("Invalid AI proposal structure");
        if (!(after.get("elementType") instanceof String type)) throw new ApiException("AI elementType is required");
        if (aiGameProposalService.supports(type)) {
            aiGameProposalService.validateSaved(projectId, item);
            return;
        }
        switch (item.getOperation()) {
            case CREATE:
                validateCreate(projectId, item);
                break;
            case UPDATE:
                if (item.getTargetId() == null || item.getTargetId() <= 0) throw new ApiException("UPDATE requires target ID");
                if (item.getTargetType() == ChangeTargetType.NODE) {
                    switch (type) {
                        case "CHARACTER": aiCharacterUpdateService.validateCharacterUpdate(projectId, item); break;
                        case "WORLD": aiWorldUpdateService.validateWorldUpdate(projectId, item); break;
                        case "LOCATION": aiLocationProposalService.validateUpdate(projectId, item); break;
                        case "WORLD_RULE": aiWorldRuleProposalService.validateUpdate(projectId, item); break;
                        case "STORY_EVENT": aiStoryEventProposalService.validateUpdate(projectId, item); break;
                        case "NODE": validateGenericUpdate(projectId, item); break;
                        default: throw new ApiException("Unsupported NODE update element type");
                    }
                } else if ((item.getTargetType() == ChangeTargetType.EDGE && "EDGE".equals(type)) || (item.getTargetType() == ChangeTargetType.RECORD && "RECORD".equals(type))) {
                    validateGenericUpdate(projectId, item);
                } else throw new ApiException("Unsupported AI update target");
                break;
            default:
                throw new ApiException("Unsupported operation");
        }
    }

    // Generic metadata updates have explicit field whitelists and snapshots.
    private void validateGenericUpdate(Integer projectId, ChangeProposalItem item) {
        Map<String, Object> before = readJsonMap(item.getBeforeJson());
        Map<String, Object> after = readJsonMap(item.getAfterJson());
        Map<String, Object> fields = extractFields(after.get("fields"));
        switch (item.getTargetType()) {
            case NODE:
                ProjectNode node = projectNodeRepository.findById(item.getTargetId()).orElseThrow(() -> new ApiException("Node not found"));
                if (!projectId.equals(node.getProjectOutput().getProject().getId()) || !"NODE".equals(node.getType())) throw new ApiException("Only ordinary NODE metadata can be updated generically");
                requireOutput(after, node.getProjectOutput().getId());
                requireAllowedFields(fields, NODE_FIELDS);
                break;
            case EDGE:
                ProjectEdge edge = projectEdgeRepository.findById(item.getTargetId()).orElseThrow(() -> new ApiException("Edge not found"));
                if (!projectId.equals(edge.getProjectOutput().getProject().getId()) || "BRANCH_CHOICE".equals(edge.getType())) throw new ApiException("Invalid generic EDGE target");
                requireOutput(after, edge.getProjectOutput().getId());
                requireAllowedFields(fields, EDGE_FIELDS);
                break;
            case RECORD:
                ProjectRecord record = projectRecordRepository.findById(item.getTargetId()).orElseThrow(() -> new ApiException("Record not found"));
                if (!projectId.equals(record.getProject().getId()) || after.get("outputId") != null) throw new ApiException("Invalid generic RECORD target");
                requireAllowedFields(fields, RECORD_FIELDS);
                break;
            default: throw new ApiException("Unsupported generic update target");
        }
        if (before.isEmpty()) throw new ApiException("Original snapshot is required");
    }

    // Load and lock the proposal before any decision
    private ChangeProposal loadAuthorizedProposal(Integer projectId, Integer authenticatedUserId, Integer proposalId) {

        if (projectId == null || authenticatedUserId == null || proposalId == null) {
            throw new ApiException("Missing proposal information");
        }


        ChangeProposal proposal = changeProposalRepository.findByIdForUpdate(proposalId).orElseThrow(() -> new ApiException("Proposal not found"));


        if (proposal == null) {
            throw new ApiException("Proposal not found");
        }

        Project project = proposal.getProject();

        if (!projectId.equals(project.getId()) || project.getOwner() == null || !authenticatedUserId.equals(project.getOwner().getId())) {

            throw new ApiException("Access denied");
        }

        // Only proposals associated with the current user's AI turn
        AIInteraction interaction = proposal.getAiInteraction();

        if (interaction == null || interaction.getUser() == null || !authenticatedUserId.equals(interaction.getUser().getId()) || !projectId.equals(interaction.getProject().getId())) {
            throw new ApiException("Invalid AI proposal owner");
        }

        return proposal;
    }


    private void requirePending(ChangeProposal proposal) {

        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new ApiException("Only pending proposals can be reviewed");
        }
    }

    // Route an approved UPDATE to its specialized handler.
    // Creative entities must never be modified through
    // generic ProjectNode metadata updates.
    private void applyUpdate(Integer projectId, ChangeProposalItem item) {

        Map<String, Object> gameAfter = readJsonMap(item.getAfterJson());
        if (aiGameProposalService.supports((String) gameAfter.get("elementType"))) {
            aiGameProposalService.applySaved(projectId, item);
            return;
        }

        // Validate the proposal target type.
        if (item.getTargetType() == null) {
            throw new ApiException("Target type is required");
        }

        // Read the saved original and proposed data.
        Map<String, Object> before = readJsonMap(item.getBeforeJson());

        Map<String, Object> after = readJsonMap(item.getAfterJson());

        // Ensure the proposal has the expected structure.
        if (!after.keySet().equals(Set.of("outputId", "elementType", "fields"))) {

            throw new ApiException("Invalid proposal data structure");
        }

        // Extract only the fields proposed for modification.
        Map<String, Object> fields = extractFields(after.get("fields"));

        switch (item.getTargetType()) {

            case NODE:

                // Validate elementType before routing.
                if (!(after.get("elementType") instanceof String elementType)) {
                    throw new ApiException("Element type is required");
                }

                // Route creative NODE updates to specialized services.
                switch (elementType) {

                    case "CHARACTER":
                        aiCharacterUpdateService.updateCharacter(projectId, item);
                        break;

                    case "WORLD":
                        aiWorldUpdateService.updateWorld(projectId, item);
                        break;

                    case "LOCATION":
                        aiLocationProposalService.applyUpdate(projectId, item);
                        break;

                    case "WORLD_RULE":
                        aiWorldRuleProposalService.applyUpdate(projectId, item);
                        break;

                    case "STORY_EVENT":
                        // Route approved event changes to the specialized service.
                        // Do not store event details in ProjectNode.dataJson.
                        aiStoryEventProposalService.applyUpdate(projectId, item);
                        break;

                    case "NODE":
                        // Only ordinary canvas metadata is allowed.
                        updateNode(projectId, item.getTargetId(), before, after, fields);
                        break;

                    default:
                        throw new ApiException("Unsupported NODE element type: " + elementType);
                }
                break;

            case EDGE:

                // Apply supported relationship metadata changes.
                updateEdge(projectId, item.getTargetId(), before, after, fields);
                break;

            case RECORD:

                // Apply supported project record changes.
                updateRecord(projectId, item.getTargetId(), before, after, fields);
                break;

            default:
                throw new ApiException("Unsupported proposal target type");
        }
    }

    // Update only safe canvas-node fields
    private void updateNode(Integer projectId, Integer nodeId, Map<String, Object> before, Map<String, Object> after, Map<String, Object> fields) {

        ProjectNode node = projectNodeRepository.findByIdForUpdate(nodeId).orElseThrow(() -> new ApiException("Node not found"));
        if (node == null || !projectId.equals(node.getProjectOutput().getProject().getId())) {

            throw new ApiException("Node does not belong to this project");
        }

        requireOutput(after, node.getProjectOutput().getId());
        requireAllowedFields(fields, NODE_FIELDS);

        Map<String, Object> current = new LinkedHashMap<>();
        current.put("type", node.getType());
        current.put("dataJson", node.getDataJson());
        current.put("positionX", node.getPositionX());
        current.put("positionY", node.getPositionY());
        current.put("outputId", node.getProjectOutput().getId());

        requireUnchanged(before, current);

        if (fields.containsKey("dataJson")) {
            node.setDataJson(optionalText(fields.get("dataJson"), 255));
        }

        if (fields.containsKey("positionX")) {
            node.setPositionX(requiredNumber(fields.get("positionX")));
        }

        if (fields.containsKey("positionY")) {
            node.setPositionY(requiredNumber(fields.get("positionY")));
        }

        node.setUpdatedAt(LocalDate.now());
        projectNodeRepository.save(node);
    }


    // Update edge labels or metadata, never its endpoints
    private void updateEdge(Integer projectId, Integer edgeId, Map<String, Object> before, Map<String, Object> after, Map<String, Object> fields) {

        ProjectEdge edge = projectEdgeRepository.findByIdForUpdate(edgeId).orElseThrow(() -> new ApiException("Edge not found"));

        if (edge == null || !projectId.equals(edge.getProjectOutput().getProject().getId())) {
            throw new ApiException("Edge does not belong to this project");
        }

        requireOutput(after, edge.getProjectOutput().getId());
        requireAllowedFields(fields, EDGE_FIELDS);

        Map<String, Object> current = new LinkedHashMap<>();
        current.put("type", edge.getType());
        current.put("label", edge.getLabel());
        current.put("dataJson", edge.getDataJson());
        current.put("sourceNodeId", edge.getSourceNode().getId());
        current.put("targetNodeId", edge.getTargetNode().getId());
        current.put("outputId", edge.getProjectOutput().getId());

        requireUnchanged(before, current);

        if (fields.containsKey("label")) {
            edge.setLabel(requiredText(fields.get("label"), 50));
        }

        if (fields.containsKey("dataJson")) {
            edge.setDataJson(optionalText(fields.get("dataJson"), 255));
        }

        projectEdgeRepository.save(edge);
    }


    // Update safe project-record fields
    private void updateRecord(Integer projectId, Integer recordId, Map<String, Object> before, Map<String, Object> after, Map<String, Object> fields) {

        ProjectRecord record = projectRecordRepository.findByIdForUpdate(recordId).orElseThrow(() -> new ApiException("Record not found"));

        if (record == null || record.getProject() == null || !projectId.equals(record.getProject().getId())) {

            throw new ApiException("Record does not belong to this project");
        }

        if (after.get("outputId") != null) {
            throw new ApiException("Record cannot specify an output");
        }

        requireAllowedFields(fields, RECORD_FIELDS);

        Map<String, Object> current = new LinkedHashMap<>();
        current.put("title", record.getTitle());
        current.put("content", record.getContent());
        current.put("type", record.getType());
        current.put("status", record.getStatus().name());
        current.put("source", record.getSource());
        current.put("version", record.getVersion());

        requireUnchanged(before, current);

        if (fields.containsKey("title")) {
            record.setTitle(requiredText(fields.get("title"), 50));
        }

        if (fields.containsKey("content")) {
            record.setContent(requiredText(fields.get("content"), 50));
        }

        if (fields.containsKey("source")) {
            record.setSource(requiredText(fields.get("source"), 50));
        }

        if (fields.containsKey("status")) {
            String status = requiredText(fields.get("status"), 50);

            try {
                record.setStatus(RecordStatus.valueOf(status));
            } catch (IllegalArgumentException e) {
                throw new ApiException("Invalid record status");
            }
        }

        record.setVersion(record.getVersion() + 1);
        record.setUpdatedAt(LocalDate.now());
        projectRecordRepository.save(record);
    }


    // Check the target has not changed since the proposal was created
    private void requireUnchanged(Map<String, Object> before, Map<String, Object> current) {

        if (!before.equals(current)) {
            throw new ApiException("Target data changed after proposal creation");
        }
    }

    private void requireOutput(Map<String, Object> after, Integer actualOutputId) {

        if (!Objects.equals(after.get("outputId"), actualOutputId)) {
            throw new ApiException("Proposal output does not match target");
        }
    }

    private void requireAllowedFields(Map<String, Object> fields, Set<String> allowed) {

        if (fields.isEmpty() || !allowed.containsAll(fields.keySet())) {
            throw new ApiException("Proposal contains unsupported or unsafe fields");
        }
    }


    // Parse stored JSON snapshots into plain maps
    private Map<String, Object> readJsonMap(String json) {

        if (json == null || json.isBlank()) {
            throw new ApiException("Missing proposal snapshot");
        }

        try {
            Map<?, ?> raw = jsonMapper.readValue(json, Map.class);

            if (raw == null) {
                throw new ApiException("Invalid proposal JSON");
            }

            Map<String, Object> result = new LinkedHashMap<>();

            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    throw new ApiException("Invalid JSON field");
                }
                result.put(key, entry.getValue());
            }

            return result;

        } catch (JacksonException e) {
            throw new ApiException("Cannot read proposal JSON");
        }
    }


    private Map<String, Object> extractFields(Object value) {

        if (!(value instanceof Map<?, ?> raw)) {
            throw new ApiException("Invalid proposed fields");
        }

        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new ApiException("Invalid proposed field name");
            }
            result.put(key, entry.getValue());
        }

        return result;
    }


    private String requiredText(Object value, int maxLength) {

        if (!(value instanceof String text) || text.isBlank() || text.length() > maxLength) {
            throw new ApiException("Invalid text value");
        }

        return text;
    }


    private String optionalText(Object value, int maxLength) {

        if (value == null) {
            return null;
        }

        if (!(value instanceof String text) || text.length() > maxLength) {

            throw new ApiException("Invalid optional text value");
        }

        return text;
    }


    private Double requiredNumber(Object value) {

        if (!(value instanceof Number number)) {
            throw new ApiException("Numeric value is required");
        }

        double result = number.doubleValue();

        if (!Double.isFinite(result)) {
            throw new ApiException("Invalid numeric value");
        }

        return result;
    }


    // Validate a CREATE proposal using its specialized handler.
    // The AI-provided elementType selects a handler, but each
    // handler must independently verify the actual proposal data.
    // Each service validates its own fields and relationships.
    private void validateCreate(
            Integer projectId,
            ChangeProposalItem item) {

        Map<String, Object> gameAfter = readJsonMap(item.getAfterJson());
        if (aiGameProposalService.supports((String) gameAfter.get("elementType"))) {
            aiGameProposalService.validateSaved(projectId, item);
            return;
        }

        // Creative CREATE operations currently target ProjectNode.
        if (item.getTargetType() != ChangeTargetType.NODE) {
            throw new ApiException("Unsupported CREATE target type");
        }

        Map<String, Object> after = readJsonMap(item.getAfterJson());

        // Reject unexpected or malformed proposal metadata.
        if (!after.keySet().equals(Set.of("outputId", "elementType", "fields"))) {

            throw new ApiException("Invalid CREATE proposal structure");
        }

        if (!(after.get("elementType") instanceof String elementType)) {
            throw new ApiException("Creative element type is required");
        }

        switch (elementType) {

            case "CHARACTER":
                aiCharacterCreationService.validateCharacterCreate(projectId, item);
                break;

            case "WORLD":
                aiWorldCreationService.validateWorldCreate(projectId, item);
                break;

            case "LOCATION":
                aiLocationProposalService.validateCreate(projectId, item);
                break;

            case "WORLD_RULE":
                aiWorldRuleProposalService.validateCreate(projectId, item);
                break;

            case "STORY_EVENT":
                // Validate all fields before creating a StoryEvent.
                aiStoryEventProposalService.validateCreate(projectId, item);
                break;

            default:
                throw new ApiException("Unsupported CREATE element type: " + elementType);
        }
    }




    // This method is called only from the approval transaction.
    // Apply a CREATE operation after explicit user approval.
    // All handlers participate in the approval transaction.
    private void applyCreate(Integer projectId, ChangeProposalItem item) {

        Map<String, Object> after = readJsonMap(item.getAfterJson());
        if (aiGameProposalService.supports((String) after.get("elementType"))) {
            aiGameProposalService.applySaved(projectId, item);
            return;
        }

        // Validate metadata again before applying changes.
        if (!after.keySet().equals(Set.of("outputId", "elementType", "fields"))) {
            throw new ApiException("Invalid CREATE proposal structure");
        }

        if (!(after.get("elementType") instanceof String elementType)) {
            throw new ApiException("Creative element type is required");
        }

        switch (elementType) {

            case "CHARACTER":
                aiCharacterCreationService.createCharacter(projectId, item);
                break;

            case "WORLD":
                aiWorldCreationService.createWorld(projectId, item);
                break;

            case "LOCATION":
                aiLocationProposalService.applyCreate(projectId, item);
                break;

            case "WORLD_RULE":
                aiWorldRuleProposalService.applyCreate(projectId, item);
                break;

            case "STORY_EVENT":
                // Create the approved event through StoryEventService.
                aiStoryEventProposalService.applyCreate(projectId, item);
                break;

            default:
                throw new ApiException("Unsupported CREATE element type: " + elementType);
        }
    }







}
