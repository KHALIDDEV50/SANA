package com.example.sana.AI.Service;

import com.example.sana.AI.Enums.WorldProposalField;
import com.example.sana.API.ApiException;
import com.example.sana.DTO.CreativityDomainDTOs.WorldDTO;

import com.example.sana.Enums.ChangeOperation;
import com.example.sana.Enums.ChangeTargetType;

import com.example.sana.Model.ChangeProposalItem;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Model.CreativityDomainModels.World;

import com.example.sana.Repository.ProjectNodeRepository;
import com.example.sana.Repository.CreativityDomainRepository.WorldRepository;

import com.example.sana.Service.CreativityDomainServise.WorldService;

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
public class AIWorldUpdateService {

    private final ProjectNodeRepository projectNodeRepository;
    private final WorldRepository worldRepository;
    private final WorldService worldService;

    private final JsonMapper jsonMapper;
    private final Validator validator;


    // Create a snapshot of the current world.
    // This snapshot is saved in ChangeProposalItem.beforeJson
    // when the AI proposal is first created.
    public String createBeforeSnapshot(Integer projectId, Integer nodeId) {

        ProjectNode node = projectNodeRepository.findById(nodeId).orElseThrow(() -> new ApiException("World node not found"));

        // Verify ownership of the selected node.
        validateProject(projectId, node);

        // Verify that the node represents a real World.
        World world = getWorld(node);

        // Serialize only the necessary fields.
        // Never serialize the JPA entity directly.
        return toJson(buildSnapshot(node, world));
    }


    // Validate a proposed world update without changing data.
    // This is called before the application phase.
    public void validateWorldUpdate(Integer projectId, ChangeProposalItem item) {
        prepareUpdate(projectId, item);
    }


    // Apply the world update after explicit user approval.
    // The caller must already have an active transaction.
    public void updateWorld(Integer projectId, ChangeProposalItem item) {

        // Revalidate the proposal immediately before execution.
        PreparedWorldUpdate prepared = prepareUpdate(projectId, item);

        // Reuse the existing Creativity Domain service.
        // It updates both World and its ProjectNode.
        worldService.updateWorld(prepared.worldId(), prepared.dto());
    }


    // Read and validate the saved proposal.
    // Build a complete WorldDTO without losing existing fields.
    private PreparedWorldUpdate prepareUpdate(Integer projectId, ChangeProposalItem item) {

        // An UPDATE must target an existing ProjectNode.
        if (item == null || item.getOperation() != ChangeOperation.UPDATE || item.getTargetType() != ChangeTargetType.NODE || item.getTargetId() == null || item.getTargetId() <= 0) {
            throw new ApiException("Invalid world update proposal");
        }

        // Lock the ProjectNode during the approval transaction.
        ProjectNode node = projectNodeRepository.findByIdForUpdate(item.getTargetId()).orElseThrow(() -> new ApiException("World node not found"));

        validateProject(projectId, node);

        World linkedWorld = getWorld(node);

        // Lock the actual World record too.
        // Node and World are stored in separate database rows.
        World world = worldRepository.findByIdForUpdate(linkedWorld.getId()).orElseThrow(() -> new ApiException("World not found"));

        // Read the original snapshot and suggested changes.
        Map<String, Object> before = readJson(item.getBeforeJson());

        Map<String, Object> after = readJson(item.getAfterJson());

        // Accept only the proposal format defined by SANA.
        if (!after.keySet().equals(Set.of("outputId", "elementType", "fields"))) {
            throw new ApiException("Invalid world proposal format");
        }

        // This service must never update a different element type.
        if (!"WORLD".equals(after.get("elementType"))) {
            throw new ApiException("This handler only supports WORLD updates");
        }

        // The output ID is optional for an existing target.
        // When provided, it must match the node's actual output.
        if (after.get("outputId") != null && !Objects.equals(after.get("outputId"), node.getProjectOutput().getId())) {

            throw new ApiException("World output does not match the proposal");
        }

        // Compare the complete world snapshot with current data.
        // Reject an outdated proposal instead of overwriting
        // changes the user made after the proposal was created.
        Map<String, Object> current = buildSnapshot(node, world);

        if (!before.equals(current)) {
            throw new ApiException("World changed after proposal creation. " + "Please review a new proposal");
        }

        // Read only the fields explicitly suggested by SANA.
        Map<String, Object> proposedFields = extractFields(after.get("fields"));

        if (proposedFields.isEmpty()) {
            throw new ApiException("World update contains no proposed fields");
        }

        // Start with the complete current WorldDTO.
        // The existing WorldService.updateWorld() replaces all
        // World fields, so we must preserve unrelated values.
        WorldDTO dto = buildWorldDTO(node, world);

        // Apply only the proposed fields to the complete DTO.
        for (Map.Entry<String, Object> entry : proposedFields.entrySet()) {

            WorldProposalField field;

            try {
                field = WorldProposalField.fromFieldName(entry.getKey());
            } catch (IllegalArgumentException e) {
                throw new ApiException("Unsupported world field: " + entry.getKey());
            }

            applyField(dto, field, entry.getValue());
        }

        // Reuse validation annotations from the team's WorldDTO.
        Set<ConstraintViolation<WorldDTO>> violations = validator.validate(dto);

        if (!violations.isEmpty()) {
            throw new ApiException("Invalid world update: " + violations.iterator().next().getMessage());
        }

        return new PreparedWorldUpdate(world.getId(), dto);
    }


    // Ensure the canvas node belongs to the requested project.
    private void validateProject(Integer projectId, ProjectNode node) {

        if (projectId == null || node.getProjectOutput() == null || node.getProjectOutput().getProject() == null || !projectId.equals(node.getProjectOutput().getProject().getId())) {

            throw new ApiException("World does not belong to this project");
        }
    }


    // Get the World linked to the selected ProjectNode.
    // Reject nodes belonging to other creativity elements.
    private World getWorld(ProjectNode node) {

        if (!"WORLD".equals(node.getType()) || node.getWorld() == null) {

            throw new ApiException("Selected node is not a world");
        }

        return node.getWorld();
    }


    // Capture both canvas metadata and all World fields.
    // SnapshotType helps us reject incompatible older formats.
    private Map<String, Object> buildSnapshot(ProjectNode node, World world) {

        Map<String, Object> data = new LinkedHashMap<>();

        // Snapshot metadata
        data.put("snapshotType", "WORLD_V1");
        data.put("nodeId", node.getId());
        data.put("worldId", world.getId());
        data.put("outputId", node.getProjectOutput().getId());

        // Canvas metadata
        data.put("nodeType", node.getType());
        data.put("nodeDataJson", node.getDataJson());
        data.put("positionX", node.getPositionX());
        data.put("positionY", node.getPositionY());

        // Complete creative-world information
        data.put("name", world.getName());
        data.put("description", world.getDescription());
        data.put("history", world.getHistory());
        data.put("lore", world.getLore());
        data.put("atmosphere", world.getAtmosphere());
        data.put("notes", world.getNotes());

        return data;
    }


    // Copy every existing value into a complete WorldDTO.
    // Fields not mentioned in the AI proposal remain unchanged.
    private WorldDTO buildWorldDTO(ProjectNode node, World world) {

        WorldDTO dto = new WorldDTO();

        // Preserve the existing canvas position.
        dto.setPositionX(node.getPositionX());
        dto.setPositionY(node.getPositionY());

        // Preserve all existing World values.
        dto.setName(world.getName());
        dto.setDescription(world.getDescription());
        dto.setHistory(world.getHistory());
        dto.setLore(world.getLore());
        dto.setAtmosphere(world.getAtmosphere());
        dto.setNotes(world.getNotes());

        return dto;
    }


    // Update a single approved field using our World enum.
    // Only known and supported fields can reach this switch.
    private void applyField(WorldDTO dto, WorldProposalField field, Object value) {

        switch (field) {

            // Canvas coordinates
            case POSITION_X ->
                    dto.setPositionX(finiteDouble(value));

            case POSITION_Y ->
                    dto.setPositionY(finiteDouble(value));

            // Required fields cannot be cleared.
            case NAME ->
                    dto.setName(requiredText(value, 50));

            case DESCRIPTION ->
                    dto.setDescription(requiredText(value, 16000));

            // Optional fields may be cleared explicitly with null.
            case HISTORY ->
                    dto.setHistory(optionalText(value, 16000));

            case LORE ->
                    dto.setLore(optionalText(value, 16000));

            case ATMOSPHERE ->
                    dto.setAtmosphere(optionalText(value, 16000));

            case NOTES ->
                    dto.setNotes(optionalText(value, 16000));
        }
    }


    // Validate non-empty text within the allowed length.
    private String requiredText(Object value, int maxLength) {

        if (!(value instanceof String text) || text.isBlank() || text.length() > maxLength) {
            throw new ApiException("Invalid required world field");
        }

        return text.trim();
    }


    // Optional text may be null when the user approves clearing it.
    private String optionalText(Object value, int maxLength) {

        if (value == null) {
            return null;
        }

        if (!(value instanceof String text) || text.length() > maxLength) {
            throw new ApiException("Invalid optional world field");
        }

        return text;
    }


    // Ensure canvas positions are finite numeric values.
    private Double finiteDouble(Object value) {

        if (!(value instanceof Number number)) {
            throw new ApiException("World position must be numeric");
        }

        double result = number.doubleValue();

        if (!Double.isFinite(result)) {
            throw new ApiException("Invalid world position");
        }

        return result;
    }


    // Read stored JSON as plain key-value data.
    // Never map AI-generated JSON directly into JPA entities.
    private Map<String, Object> readJson(String json) {

        if (json == null || json.isBlank()) {
            throw new ApiException("Missing world proposal data");
        }

        try {
            Map<?, ?> raw = jsonMapper.readValue(json, Map.class);

            if (raw == null) {
                throw new ApiException("Invalid world JSON");
            }

            return stringKeyMap(raw);

        } catch (JacksonException e) {
            throw new ApiException("Cannot read world proposal JSON");
        }
    }


    // Extract the proposed fields from afterJson.
    private Map<String, Object> extractFields(Object value) {

        if (!(value instanceof Map<?, ?> raw)) {
            throw new ApiException("Invalid proposed world fields");
        }

        return stringKeyMap(raw);
    }


    // Ensure all JSON object keys are strings.
    private Map<String, Object> stringKeyMap(Map<?, ?> raw) {

        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : raw.entrySet()) {

            if (!(entry.getKey() instanceof String key)) {
                throw new ApiException("Invalid world JSON field name");
            }

            result.put(key, entry.getValue());
        }

        return result;
    }


    // Serialize snapshots as JSON without exposing JPA entities.
    private String toJson(Object data) {
        try {
            return jsonMapper.writeValueAsString(data);

        } catch (JacksonException e) {
            throw new ApiException("Cannot serialize world snapshot");
        }
    }


    // Validated information used only during an approval.
    // This record is not a database entity or a new table.
    private record PreparedWorldUpdate(Integer worldId, WorldDTO dto) {}
}
