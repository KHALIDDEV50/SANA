package com.example.sana.AI.Service;

import com.example.sana.AI.Enums.LocationProposalField;
import com.example.sana.API.ApiException;
import com.example.sana.DTO.CreativityDomainDTOs.LocationDTO;

import com.example.sana.Enums.ChangeOperation;
import com.example.sana.Enums.ChangeTargetType;

import com.example.sana.Model.ChangeProposalItem;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Model.CreativityDomainModels.Location;

import com.example.sana.Repository.ProjectNodeRepository;
import com.example.sana.Repository.ProjectOutputRepository;
import com.example.sana.Repository.CreativityDomainRepository.LocationRepository;

import com.example.sana.Service.CreativityDomainServise.LocationService;

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
public class AILocationProposalService {

    private final LocationService locationService;
    private final LocationRepository locationRepository;
    private final ProjectNodeRepository projectNodeRepository;
    private final ProjectOutputRepository projectOutputRepository;

    private final JsonMapper jsonMapper;
    private final Validator validator;


    // -------------------------------------------------------
    // CREATE LOCATION
    // -------------------------------------------------------

    // Validate a creation proposal without writing to the database.
    public void validateCreate(Integer projectId, ChangeProposalItem item) {
        prepareCreate(projectId, item);
    }


    // Execute creation only from the approved proposal transaction.
    public void applyCreate(Integer projectId, ChangeProposalItem item) {

        PreparedCreate prepared = prepareCreate(projectId, item);

        // Reuse the original service so world and parent-location
        // relationships receive the team's existing validations.
        locationService.addLocation(prepared.outputId(), prepared.worldId(), prepared.parentLocationId(), prepared.dto());
    }


    private PreparedCreate prepareCreate(Integer projectId, ChangeProposalItem item) {

        // CREATE must not supply an existing target ID.
        requireItem(item, ChangeOperation.CREATE);

        if (item.getTargetId() != null || item.getBeforeJson() != null) {
            throw new ApiException("Invalid location creation target");
        }

        Map<String, Object> after = readProposal(item);
        requireLocationType(after);

        // Resolve the destination output from the saved proposal.
        Integer outputId = positiveId(after.get("outputId"));

        validateOutput(projectId, outputId);

        Map<String, Object> fields = extractFields(after);
        validateFieldNames(fields);

        // Relationships are optional, and their IDs reference
        // existing entities rather than creating new records.
        Integer worldId = optionalId(fields.get("worldId"));
        Integer parentId = optionalId(fields.get("parentLocationId"));

        LocationDTO dto = new LocationDTO();

        // Required canvas position and location name.
        dto.setPositionX(finiteDouble(fields.get("positionX")));
        dto.setPositionY(finiteDouble(fields.get("positionY")));
        dto.setName(requiredText(fields.get("name"), 50));

        // Optional descriptive fields.
        dto.setLocationType(optionalText(fields.get("locationType"), 50));
        dto.setNarrativeSignificance(optionalText(fields.get("narrativeSignificance"), 16000));
        dto.setNotes(optionalText(fields.get("notes"), 16000));

        validateDTO(dto);

        return new PreparedCreate(outputId, worldId, parentId, dto);
    }


    // -------------------------------------------------------
    // UPDATE LOCATION
    // -------------------------------------------------------

    // Capture both the Location fields and its canvas metadata.
    // The snapshot is saved in the existing beforeJson column.
    public String createBeforeSnapshot(Integer projectId, Integer nodeId) {

        ProjectNode node = projectNodeRepository.findById(nodeId).orElseThrow(() -> new ApiException("Location node not found"));

        validateNodeProject(projectId, node);

        Location location = getLocation(node);

        // Return plain JSON, not a serialized JPA entity.
        return toJson(buildSnapshot(node, location));
    }


    // Validate a proposed update before applying any changes.
    public void validateUpdate(Integer projectId, ChangeProposalItem item) {

        prepareUpdate(projectId, item);
    }


    // Apply only the explicitly approved fields.
    public void applyUpdate(Integer projectId, ChangeProposalItem item) {

        PreparedUpdate prepared = prepareUpdate(projectId, item);

        // The existing LocationService replaces DTO fields.
        // Our DTO already contains all unchanged original values.
        locationService.updateLocation(prepared.locationId(), prepared.dto());
    }


    private PreparedUpdate prepareUpdate(Integer projectId, ChangeProposalItem item) {

        requireItem(item, ChangeOperation.UPDATE);

        if (item.getTargetId() == null || item.getTargetId() <= 0) {
            throw new ApiException("Location target ID is required");
        }

        // The proposal target ID represents ProjectNode.id.
        ProjectNode node = projectNodeRepository.findByIdForUpdate(item.getTargetId()).orElseThrow(() -> new ApiException("Location node not found"));

        validateNodeProject(projectId, node);

        Location linkedLocation = getLocation(node);

        // Lock the Location row itself because ProjectNode and
        // Location are stored in different database tables.
        Location location = locationRepository.findByIdForUpdate(linkedLocation.getId()).orElseThrow(() -> new ApiException("Location not found"));

        Map<String, Object> before = readJson(item.getBeforeJson());

        Map<String, Object> after = readProposal(item);
        requireLocationType(after);

        // An optional output ID must match the actual target.
        if (after.get("outputId") != null && !Objects.equals(optionalId(after.get("outputId")), node.getProjectOutput().getId())) {
            throw new ApiException("Location output does not match");
        }

        // Reject stale proposals before modifying the location.
        // This includes changes to the location's relationships.
        if (!before.equals(buildSnapshot(node, location))) {
            throw new ApiException("Location changed after proposal creation");
        }

        Map<String, Object> fields = extractFields(after);
        validateFieldNames(fields);

        if (fields.isEmpty()) {
            throw new ApiException("Location update has no fields");
        }

        // World and parent changes need a separate relationship flow.
        // The regular LocationService.updateLocation() does not
        // update these relationships, so reject them explicitly.
        if (fields.containsKey("worldId") || fields.containsKey("parentLocationId")) {
            throw new ApiException("Location relationship updates are not supported here");
        }

        // Copy the complete current state before modifying fields.
        LocationDTO dto = new LocationDTO();

        dto.setPositionX(node.getPositionX());
        dto.setPositionY(node.getPositionY());
        dto.setName(location.getName());
        dto.setLocationType(location.getLocationType());
        dto.setNarrativeSignificance(location.getNarrativeSignificance());
        dto.setNotes(location.getNotes());

        // Apply only fields explicitly approved by the user.
        // Missing fields retain their original values.
        for (Map.Entry<String, Object> entry : fields.entrySet()) {

            LocationProposalField field = LocationProposalField.fromFieldName(entry.getKey());

            switch (field) {

                case POSITION_X ->
                        dto.setPositionX(finiteDouble(entry.getValue()));

                case POSITION_Y ->
                        dto.setPositionY(finiteDouble(entry.getValue()));

                case NAME ->
                        dto.setName(requiredText(entry.getValue(), 50));

                case LOCATION_TYPE ->
                        dto.setLocationType(optionalText(entry.getValue(), 50));

                case NARRATIVE_SIGNIFICANCE ->
                        dto.setNarrativeSignificance(optionalText(entry.getValue(), 16000));

                case NOTES ->
                        dto.setNotes(optionalText(entry.getValue(), 16000));

                // Relationship modifications must not pass through
                // the ordinary location information update.
                case WORLD_ID, PARENT_LOCATION_ID ->
                        throw new ApiException("Unsupported location relationship update");
            }
        }

        validateDTO(dto);

        return new PreparedUpdate(location.getId(), dto);
    }


    // -------------------------------------------------------
    // SNAPSHOT AND OWNERSHIP VALIDATION
    // -------------------------------------------------------

    private Map<String, Object> buildSnapshot(ProjectNode node, Location location) {

        Map<String, Object> snapshot = new LinkedHashMap<>();

        // Snapshot metadata prevents accepting an older format.
        snapshot.put("snapshotType", "LOCATION_V1");

        // Canvas identity and current state.
        snapshot.put("nodeId", node.getId());
        snapshot.put("locationId", location.getId());
        snapshot.put("outputId", node.getProjectOutput().getId());
        snapshot.put("nodeType", node.getType());
        snapshot.put("nodeDataJson", node.getDataJson());
        snapshot.put("positionX", node.getPositionX());
        snapshot.put("positionY", node.getPositionY());

        // All location fields.
        snapshot.put("name", location.getName());
        snapshot.put("locationType", location.getLocationType());
        snapshot.put("narrativeSignificance", location.getNarrativeSignificance());
        snapshot.put("notes", location.getNotes());

        // Include relationship IDs to detect parent/world changes.
        snapshot.put("worldId", location.getWorld() == null ? null : location.getWorld().getId());

        snapshot.put("parentLocationId", location.getParentLocation() == null ? null : location.getParentLocation().getId());

        return snapshot;
    }


    private void validateNodeProject(Integer projectId, ProjectNode node) {

        if (projectId == null || node.getProjectOutput() == null || node.getProjectOutput().getProject() == null || !projectId.equals(node.getProjectOutput().getProject().getId())) {

            throw new ApiException("Location does not belong to this project");
        }
    }


    private Location getLocation(ProjectNode node) {

        // Never trust the elementType supplied by an AI proposal
        // without checking the real entity's type and association.
        if (!"LOCATION".equals(node.getType()) || node.getLocation() == null) {
            throw new ApiException("Selected node is not a location");
        }

        return node.getLocation();
    }


    private void validateOutput(Integer projectId, Integer outputId) {

        ProjectOutput output = projectOutputRepository.findProjectOutputById(outputId);

        if (output == null || output.getProject() == null || !projectId.equals(output.getProject().getId())) {

            throw new ApiException("Output does not belong to the project");
        }
    }


    // -------------------------------------------------------
    // JSON AND FIELD VALIDATION HELPERS
    // -------------------------------------------------------

    private void requireItem(ChangeProposalItem item, ChangeOperation expected) {

        if (item == null || item.getOperation() != expected || item.getTargetType() != ChangeTargetType.NODE) {

            throw new ApiException("Invalid location proposal operation");
        }
    }


    private Map<String, Object> readProposal(ChangeProposalItem item) {

        Map<String, Object> after = readJson(item.getAfterJson());

        // Keep the exact format used by AIPendingProposalService.
        if (!after.keySet().equals(Set.of("outputId", "elementType", "fields"))) {
            throw new ApiException("Invalid proposal format");
        }

        return after;
    }


    private void requireLocationType(Map<String, Object> after) {

        if (!"LOCATION".equals(after.get("elementType"))) {
            throw new ApiException("This handler supports LOCATION only");
        }
    }


    private Map<String, Object> extractFields(Map<String, Object> after) {

        if (!(after.get("fields") instanceof Map<?, ?> raw)) {
            throw new ApiException("Invalid location fields");
        }

        return stringKeyMap(raw);
    }


    private void validateFieldNames(Map<String, Object> fields) {

        // Reject unknown fields before processing any value.
        // This prevents modifications to IDs and JPA relations.
        if (!fields.keySet().stream().allMatch(LocationProposalField::isAllowed)) {

            throw new ApiException("Location proposal contains unsupported fields");
        }
    }


    private void validateDTO(LocationDTO dto) {

        // Apply the existing DTO's Jakarta Validation constraints.
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(dto);

        if (!violations.isEmpty()) {
            throw new ApiException(violations.iterator().next().getMessage());
        }
    }


    private Map<String, Object> readJson(String json) {

        if (json == null || json.isBlank()) {
            throw new ApiException("Missing location proposal data");
        }

        try {
            Map<?, ?> raw = jsonMapper.readValue(json, Map.class);

            if (raw == null) {
                throw new ApiException("Invalid location JSON");
            }

            return stringKeyMap(raw);

        } catch (JacksonException e) {
            throw new ApiException("Cannot read location JSON");
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
            throw new ApiException(
                    "Cannot serialize location snapshot");
        }
    }


    private String requiredText(Object value, int maxLength) {

        if (!(value instanceof String text) || text.isBlank() || text.length() > maxLength) {

            throw new ApiException("Invalid required location text");
        }

        return text.trim();
    }


    private String optionalText(Object value, int maxLength) {

        if (value == null) {
            return null;
        }

        if (!(value instanceof String text) || text.length() > maxLength) {

            throw new ApiException("Invalid optional location text");
        }

        return text;
    }


    private Integer positiveId(Object value) {

        if (!(value instanceof Number number)) {
            throw new ApiException("Location reference ID is required");
        }

        double n = number.doubleValue();

        if (!Double.isFinite(n) || n < 1 || n > Integer.MAX_VALUE || Math.floor(n) != n) {

            throw new ApiException("Invalid location reference ID");
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
            throw new ApiException("Invalid location position");
        }

        return n;
    }


    // These are ordinary Java records, not JPA entities.
    private record PreparedCreate(Integer outputId, Integer worldId, Integer parentLocationId, LocationDTO dto) {}

    private record PreparedUpdate(Integer locationId, LocationDTO dto) {}
}
