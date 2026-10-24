package com.example.sana.AI.Service;

import com.example.sana.AI.Enums.WorldRuleProposalField;
import com.example.sana.API.ApiException;
import com.example.sana.DTO.CreativityDomainDTOs.WorldRuleDTO;

import com.example.sana.Enums.ChangeOperation;
import com.example.sana.Enums.ChangeTargetType;

import com.example.sana.Model.ChangeProposalItem;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Model.ProjectOutput;

import com.example.sana.Model.CreativityDomainModels.World;
import com.example.sana.Model.CreativityDomainModels.WorldRule;

import com.example.sana.Repository.ProjectNodeRepository;
import com.example.sana.Repository.ProjectOutputRepository;
import com.example.sana.Repository.CreativityDomainRepository.WorldRepository;
import com.example.sana.Repository.CreativityDomainRepository.WorldRuleRepository;

import com.example.sana.Service.CreativityDomainServise.WorldRuleService;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AIWorldRuleProposalService {

    private final WorldRuleService worldRuleService;
    private final WorldRuleRepository worldRuleRepository;
    private final WorldRepository worldRepository;
    private final ProjectNodeRepository projectNodeRepository;
    private final ProjectOutputRepository projectOutputRepository;

    private final JsonMapper jsonMapper;
    private final Validator validator;


    // =====================================================
    // CREATE WORLD RULE
    // =====================================================

    // Validate a WorldRule creation proposal.
    // No database records are created during validation.
    public void validateCreate(Integer projectId, ChangeProposalItem item) {
        prepareCreate(projectId, item);
    }


    // Create a WorldRule after the user approves the proposal.
    // The approval service controls the database transaction.
    public void applyCreate(Integer projectId, ChangeProposalItem item) {
        // Validate all proposed data before saving anything.
        PreparedCreate prepared = prepareCreate(projectId, item);

        // Reuse the existing Creativity Domain service.
        // It creates the ProjectNode and WorldRule together.
        worldRuleService.addWorldRule(prepared.outputId(), prepared.worldId(), prepared.dto());
    }


    // Convert a CREATE proposal into a validated WorldRuleDTO.
    private PreparedCreate prepareCreate(Integer projectId, ChangeProposalItem item) {
        requireOperation(item, ChangeOperation.CREATE);

        // CREATE must not reference an existing target.
        if (item.getTargetId() != null || item.getBeforeJson() != null) {

            throw new ApiException("Invalid WorldRule creation target");
        }

        Map<String, Object> after = readProposal(item);

        Integer outputId = positiveId(after.get("outputId"));

        // Confirm that the destination output belongs to
        // the project whose proposal is being approved.
        validateOutput(projectId, outputId);

        Map<String, Object> fields = extractFields(after);

        validateFieldNames(fields);

        // World association is optional during creation.
        Integer worldId = optionalId(fields.get("worldId"));

        // Validate the World relationship before approval.
        // WorldRule and World must belong to the same output.
        if (worldId != null) {

            World world = worldRepository.findWorldById(worldId);

            if (world == null || world.getProjectNode() == null || world.getProjectNode().getProjectOutput() == null || !outputId.equals(world.getProjectNode().getProjectOutput().getId())) {

                throw new ApiException("World does not belong to this output");
            }
        }

        // Build a DTO instead of creating a JPA entity directly.
        WorldRuleDTO dto = new WorldRuleDTO();

        dto.setPositionX(finiteDouble(fields.get("positionX")));
        dto.setPositionY(finiteDouble(fields.get("positionY")));

        dto.setName(requiredText(fields.get("name"), 50));
        dto.setDescription(requiredText(fields.get("description"), 16000));

        dto.setScope(optionalText(fields.get("scope"), 50));
        dto.setConditions(optionalText(fields.get("conditions"), 50));
        dto.setExceptions(optionalText(fields.get("exceptions"), 50));
        dto.setConsequences(optionalText(fields.get("consequences"), 50));
        dto.setNotes(optionalText(fields.get("notes"), 16000));

        // Apply the existing WorldRuleDTO validation rules.
        validateDTO(dto);

        return new PreparedCreate(outputId, worldId, dto);
    }


    // =====================================================
    // UPDATE WORLD RULE
    // =====================================================

    // Save a full snapshot when an UPDATE proposal is created.
    // This data will be stored in ChangeProposalItem.beforeJson.
    public String createBeforeSnapshot(Integer projectId, Integer nodeId) {

        ProjectNode node = projectNodeRepository.findById(nodeId).orElseThrow(() -> new ApiException("WorldRule node not found"));

        validateNodeProject(projectId, node);

        WorldRule rule = getWorldRule(node);

        // Store plain data rather than serializing a JPA entity.
        return toJson(buildSnapshot(node, rule));
    }


    // Validate an UPDATE proposal without applying changes.
    public void validateUpdate(Integer projectId, ChangeProposalItem item) {
        prepareUpdate(projectId, item);
    }


    // Apply only the approved changes to an existing WorldRule.
    public void applyUpdate(Integer projectId, ChangeProposalItem item) {

        PreparedUpdate prepared = prepareUpdate(projectId, item);

        // Reuse the existing update service.
        // The DTO already includes all unchanged values.
        worldRuleService.updateWorldRule(prepared.ruleId(), prepared.dto());
    }


    // Prepare a complete DTO with only the requested changes.
    private PreparedUpdate prepareUpdate(Integer projectId, ChangeProposalItem item) {

        requireOperation(item, ChangeOperation.UPDATE);

        if (item.getTargetId() == null || item.getTargetId() <= 0) {

            throw new ApiException("WorldRule target ID is required");
        }

        // The proposal target is a ProjectNode ID.
        // Lock the node while applying an approved change.
        ProjectNode node = projectNodeRepository.findByIdForUpdate(item.getTargetId()).orElseThrow(() -> new ApiException("WorldRule node not found"));

        validateNodeProject(projectId, node);

        WorldRule linkedRule = getWorldRule(node);

        // WorldRule is stored in a separate database row.
        // Lock that row as well during the approval transaction.
        WorldRule rule = worldRuleRepository.findByIdForUpdate(linkedRule.getId()).orElseThrow(() -> new ApiException("WorldRule not found"));

        Map<String, Object> before = readJson(item.getBeforeJson());

        Map<String, Object> after = readProposal(item);

        // If the proposal contains an output ID,
        // verify that it matches the actual node output.
        if (after.get("outputId") != null && !Objects.equals(optionalId(after.get("outputId")), node.getProjectOutput().getId())) {

            throw new ApiException("WorldRule output does not match");
        }

        // Compare the saved snapshot against current database data.
        // Reject outdated proposals instead of overwriting user edits.
        if (!before.equals(buildSnapshot(node, rule))) {

            throw new ApiException("WorldRule changed after proposal creation");
        }

        Map<String, Object> fields = extractFields(after);

        validateFieldNames(fields);

        if (fields.isEmpty()) {
            throw new ApiException("WorldRule update has no fields");
        }

        // Changing the linked World is a relationship operation.
        // The regular WorldRule update service does not handle it.
        if (fields.containsKey("worldId")) {
            throw new ApiException("World relationship updates are not supported here");
        }

        // Copy every existing field before applying new values.
        // This prevents a partial AI suggestion from clearing
        // unrelated WorldRule information.
        WorldRuleDTO dto = new WorldRuleDTO();

        dto.setPositionX(node.getPositionX());
        dto.setPositionY(node.getPositionY());

        dto.setName(rule.getName());
        dto.setDescription(rule.getDescription());
        dto.setScope(rule.getScope());
        dto.setConditions(rule.getConditions());
        dto.setExceptions(rule.getExceptions());
        dto.setConsequences(rule.getConsequences());
        dto.setNotes(rule.getNotes());

        // Apply only fields included in the approved proposal.
        // We use the traditional switch/case syntax.
        for (Map.Entry<String, Object> entry : fields.entrySet()) {

            WorldRuleProposalField field = WorldRuleProposalField.fromFieldName(entry.getKey());

            Object value = entry.getValue();

            switch (field) {

                case POSITION_X:
                    dto.setPositionX(finiteDouble(value));
                    break;

                case POSITION_Y:
                    dto.setPositionY(finiteDouble(value));
                    break;

                case NAME:
                    dto.setName(requiredText(value, 50));
                    break;

                case DESCRIPTION:
                    dto.setDescription(requiredText(value, 16000));
                    break;

                case SCOPE:
                    dto.setScope(optionalText(value, 50));
                    break;

                case CONDITIONS:
                    dto.setConditions(optionalText(value, 50));
                    break;

                case EXCEPTIONS:
                    dto.setExceptions(optionalText(value, 50));
                    break;

                case CONSEQUENCES:
                    dto.setConsequences(optionalText(value, 50));
                    break;

                case NOTES:
                    dto.setNotes(optionalText(value, 16000));
                    break;

                case WORLD_ID:
                    // Relationship changes require a dedicated handler.
                    throw new ApiException("Cannot change World through this update");

                default:
                    throw new ApiException("Unsupported WorldRule field");
            }
        }

        // Validate the final DTO after merging the changes.
        validateDTO(dto);

        return new PreparedUpdate(rule.getId(), dto);
    }


    // =====================================================
    // SNAPSHOT HELPERS
    // =====================================================

    // Include the rule, its node, and its World relationship.
    // This allows us to detect changes after proposal creation.
    private Map<String, Object> buildSnapshot(ProjectNode node, WorldRule rule) {

        Map<String, Object> data = new LinkedHashMap<>();

        // Snapshot metadata
        data.put("snapshotType", "WORLD_RULE_V1");

        // ProjectNode information
        data.put("nodeId", node.getId());
        data.put("ruleId", rule.getId());
        data.put("outputId", node.getProjectOutput().getId());
        data.put("nodeType", node.getType());
        data.put("nodeDataJson", node.getDataJson());
        data.put("positionX", node.getPositionX());
        data.put("positionY", node.getPositionY());

        // Actual WorldRule information
        data.put("name", rule.getName());
        data.put("description", rule.getDescription());
        data.put("scope", rule.getScope());
        data.put("conditions", rule.getConditions());
        data.put("exceptions", rule.getExceptions());
        data.put("consequences", rule.getConsequences());
        data.put("notes", rule.getNotes());

        // Track relationship changes without updating the relationship.
        data.put("worldId", rule.getWorld() == null ? null : rule.getWorld().getId());
        return data;
    }


    // =====================================================
    // PROJECT AND ENTITY VALIDATION
    // =====================================================

    private void validateOutput(Integer projectId, Integer outputId) {

        ProjectOutput output = projectOutputRepository.findProjectOutputById(outputId);

        if (projectId == null || output == null || output.getProject() == null || !projectId.equals(output.getProject().getId())) {

            throw new ApiException("Output does not belong to this project");
        }
    }


    private void validateNodeProject(Integer projectId, ProjectNode node) {

        if (projectId == null || node.getProjectOutput() == null || node.getProjectOutput().getProject() == null || !projectId.equals(node.getProjectOutput().getProject().getId())) {

            throw new ApiException("WorldRule does not belong to this project");
        }
    }


    private WorldRule getWorldRule(ProjectNode node) {

        // Never trust the AI's elementType alone.
        // Verify the node and its actual linked WorldRule.
        if (!"WORLD_RULE".equals(node.getType()) || node.getWorldRule() == null) {
            throw new ApiException("Selected node is not a WorldRule");
        }

        return node.getWorldRule();
    }


    private void requireOperation(ChangeProposalItem item, ChangeOperation expected) {

        if (item == null || item.getOperation() != expected || item.getTargetType() != ChangeTargetType.NODE) {
            throw new ApiException("Invalid WorldRule proposal operation");
        }
    }


    // =====================================================
    // JSON VALIDATION HELPERS
    // =====================================================

    private Map<String, Object> readProposal(ChangeProposalItem item) {

        Map<String, Object> after = readJson(item.getAfterJson());

        // Use the exact structure created by AIPendingProposalService.
        if (!after.keySet().equals(Set.of("outputId", "elementType", "fields"))) {

            throw new ApiException("Invalid WorldRule proposal format");
        }

        if (!"WORLD_RULE".equals(after.get("elementType"))) {

            throw new ApiException("This handler supports WORLD_RULE only");
        }

        return after;
    }


    private Map<String, Object> extractFields(Map<String, Object> after) {

        if (!(after.get("fields") instanceof Map<?, ?> raw)) {
            throw new ApiException("Invalid WorldRule fields");
        }

        return stringKeyMap(raw);
    }


    private void validateFieldNames(Map<String, Object> fields) {

        // Reject unknown field names before using their values.
        if (!fields.keySet().stream().allMatch(WorldRuleProposalField::isAllowed)) {

            throw new ApiException("WorldRule proposal contains unsupported fields");
        }
    }


    private void validateDTO(WorldRuleDTO dto) {

        // Reuse the annotations already defined on WorldRuleDTO.
        Set<ConstraintViolation<WorldRuleDTO>> violations = validator.validate(dto);

        if (!violations.isEmpty()) {
            throw new ApiException(violations.iterator().next().getMessage());
        }
    }


    private Map<String, Object> readJson(String json) {

        if (json == null || json.isBlank()) {
            throw new ApiException("Missing WorldRule proposal data");
        }

        try {
            Map<?, ?> raw = jsonMapper.readValue(json, Map.class);

            if (raw == null) {
                throw new ApiException("Invalid WorldRule JSON");
            }

            return stringKeyMap(raw);

        } catch (JacksonException e) {
            throw new ApiException("Cannot read WorldRule JSON");
        }
    }


    private Map<String, Object> stringKeyMap(Map<?, ?> raw) {

        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : raw.entrySet()) {

            if (!(entry.getKey() instanceof String key)) {
                throw new ApiException("Invalid JSON field name");
            }

            result.put(key, entry.getValue());
        }

        return result;
    }


    private String toJson(Map<String, Object> data) {

        try {
            return jsonMapper.writeValueAsString(data);

        } catch (JacksonException e) {
            throw new ApiException("Cannot serialize WorldRule snapshot");
        }
    }


    // =====================================================
    // FIELD VALIDATION HELPERS
    // =====================================================

    private String requiredText(Object value, int maxLength) {

        if (!(value instanceof String text) || text.isBlank() || text.length() > maxLength) {
            throw new ApiException("Invalid required WorldRule field");
        }

        return text.trim();
    }


    private String optionalText(Object value, int maxLength) {

        if (value == null) {
            return null;
        }

        if (!(value instanceof String text) || text.length() > maxLength) {
            throw new ApiException("Invalid optional WorldRule field");
        }

        return text;
    }


    private Integer positiveId(Object value) {

        if (!(value instanceof Number number)) {
            throw new ApiException("WorldRule reference ID is required");
        }

        double n = number.doubleValue();

        if (!Double.isFinite(n) || n < 1 || n > Integer.MAX_VALUE || Math.floor(n) != n) {
            throw new ApiException("Invalid WorldRule reference ID");
        }

        return number.intValue();
    }


    private Integer optionalId(Object value) {
        return value == null ? null : positiveId(value);
    }


    private Double finiteDouble(Object value) {

        if (!(value instanceof Number number)) {
            throw new ApiException("Numeric position is required");
        }

        double n = number.doubleValue();

        if (!Double.isFinite(n)) {
            throw new ApiException("Invalid WorldRule position");
        }

        return n;
    }


    // =====================================================
    // INTERNAL DATA
    // =====================================================

    // Ordinary Java records used inside this service.
    // They do not create database entities or tables.
    private record PreparedCreate(Integer outputId, Integer worldId, WorldRuleDTO dto) {}

    private record PreparedUpdate(Integer ruleId, WorldRuleDTO dto) {}
}
