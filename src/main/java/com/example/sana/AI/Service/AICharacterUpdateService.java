
package com.example.sana.AI.Service;

import com.example.sana.AI.Enums.CharacterProposalField;
import com.example.sana.API.ApiException;
import com.example.sana.DTO.CreativityDomainDTOs.CharacterProfileDTO;

import com.example.sana.Enums.ChangeOperation;
import com.example.sana.Enums.ChangeTargetType;

import com.example.sana.Model.ChangeProposalItem;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Model.CreativityDomainModels.CharacterProfile;

import com.example.sana.Repository.ProjectNodeRepository;
import com.example.sana.Repository.CreativityDomainRepository.CharacterProfileRepository;

import com.example.sana.Service.CreativityDomainServise.CharacterProfileService;

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
public class AICharacterUpdateService {

    private final ProjectNodeRepository projectNodeRepository;
    private final CharacterProfileRepository characterProfileRepository;
    private final CharacterProfileService characterProfileService;

    private final JsonMapper jsonMapper;
    private final Validator validator;


    // Create a full character snapshot when a proposal is saved.
    // The snapshot will later protect the user's newer edits
    // from being overwritten by an outdated AI suggestion.
    public String createBeforeSnapshot(Integer projectId, Integer nodeId) {

        ProjectNode node = projectNodeRepository.findById(nodeId).orElseThrow(() -> new ApiException("Character node not found"));
        // Never allow a snapshot from another project.
        validateProject(projectId, node);

        CharacterProfile character = getCharacter(node);

        // Store the snapshot in the existing beforeJson column.
        // No additional database table is required.
        return toJson(buildSnapshot(node, character));
    }


    // Validate an UPDATE without applying any changes.
    // The approval service calls this during its validation phase.
    public void validateCharacterUpdate(Integer projectId, ChangeProposalItem item) {
        prepareUpdate(projectId, item);
    }


    // Apply the approved character update.
    // This method must be called inside the approval transaction.
    public void updateCharacter(Integer projectId, ChangeProposalItem item) {

        // Validate the saved proposal and merge only approved fields.
        PreparedUpdate prepared = prepareUpdate(projectId, item);

        // Reuse the existing Creativity Domain update logic.
        // The DTO contains all existing character values plus
        // the approved changes, so unrelated fields are preserved.
        characterProfileService.updateCharacterProfile(prepared.characterId(), prepared.dto());
    }


    // Validate the proposal and prepare a complete update DTO.
    private PreparedUpdate prepareUpdate(Integer projectId, ChangeProposalItem item) {

        // A character update targets its ProjectNode ID.
        // It must not create or delete an entity.
        if (item == null || item.getOperation() != ChangeOperation.UPDATE || item.getTargetType() != ChangeTargetType.NODE || item.getTargetId() == null || item.getTargetId() <= 0) {

            throw new ApiException("Invalid character update proposal");
        }

        // Lock the canvas node during the approval transaction.
        ProjectNode node = projectNodeRepository.findByIdForUpdate(item.getTargetId()).orElseThrow(() -> new ApiException("Character node not found"));

        validateProject(projectId, node);

        CharacterProfile linkedCharacter = getCharacter(node);

        // Lock the character record itself before checking its data.
        // This protects concurrent AI updates to the same character.
        CharacterProfile character = characterProfileRepository.findByIdForUpdate(linkedCharacter.getId()).orElseThrow(() -> new ApiException("Character not found"));

        // Read the snapshots saved when the proposal was created.
        Map<String, Object> before = readJson(item.getBeforeJson());

        Map<String, Object> after = readJson(item.getAfterJson());

        // Old or unexpected proposal formats must never be applied.
        // The update must contain the expected metadata structure.
        if (!after.keySet().equals(Set.of("outputId", "elementType", "fields"))) {

            throw new ApiException("Invalid character proposal format");
        }

        if (!"CHARACTER".equals(after.get("elementType"))) {
            throw new ApiException("This handler supports CHARACTER only");
        }

        // If the proposal specifies an output, verify its ownership.
        Object outputId = after.get("outputId");

        if (outputId != null && !Objects.equals(outputId, node.getProjectOutput().getId())) {

            throw new ApiException("Character output does not match proposal");
        }

        // Compare ALL character details, not only ProjectNode fields.
        // This also catches changes to fields that the AI did not edit.
        Map<String, Object> current = buildSnapshot(node, character);

        if (!before.equals(current)) {
            throw new ApiException("Character changed after proposal creation. " + "Please review a new proposal");
        }

        // Extract only the changes explicitly proposed by SANA.
        Map<String, Object> proposedFields = getProposedFields(after.get("fields"));

        if (proposedFields.isEmpty()) {
            throw new ApiException("Character update has no proposed fields");
        }

        // Create a complete DTO from the existing saved character.
        // This is important because the regular update method
        // replaces every field with the values in the DTO.
        CharacterProfileDTO dto = buildCharacterDTO(node, character);

        // Update ONLY fields explicitly included in the proposal.
        // All other values remain unchanged in the DTO.
        for (Map.Entry<String, Object> entry : proposedFields.entrySet()) {

            CharacterProposalField field;

            try {
                field = CharacterProposalField.fromFieldName(
                        entry.getKey());
            } catch (IllegalArgumentException e) {
                throw new ApiException("Unsupported character field: " + entry.getKey());
            }

            applyField(dto, field, entry.getValue());
        }

        // Reuse Jakarta Validation annotations already defined
        // on the team's CharacterProfileDTO.
        Set<ConstraintViolation<CharacterProfileDTO>> violations = validator.validate(dto);

        if (!violations.isEmpty()) {
            throw new ApiException("Invalid character update: " + violations.iterator().next().getMessage());
        }

        return new PreparedUpdate(character.getId(), dto);
    }


    // Confirm the selected node belongs to the requested project.
    private void validateProject(Integer projectId, ProjectNode node) {

        if (projectId == null || node.getProjectOutput() == null || node.getProjectOutput().getProject() == null || !projectId.equals(node.getProjectOutput().getProject().getId())) {

            throw new ApiException("Character does not belong to this project");
        }
    }


    // Get the CharacterProfile attached to a canvas node.
    private CharacterProfile getCharacter(ProjectNode node) {

        if (!"CHARACTER".equals(node.getType()) || node.getCharacterProfile() == null) {

            throw new ApiException("Selected node is not a character");
        }

        return node.getCharacterProfile();
    }


    // Build the snapshot used for conflict detection.
    // Include the character's details AND important node metadata.
    // Do not serialize JPA entities directly into the snapshot.
    private Map<String, Object> buildSnapshot(ProjectNode node, CharacterProfile c) {

        Map<String, Object> data = new LinkedHashMap<>();

        // A format identifier helps us reject incompatible old snapshots.
        data.put("snapshotType", "CHARACTER_PROFILE_V1");

        // Canvas identity and current position.
        data.put("nodeId", node.getId());
        data.put("characterId", c.getId());
        data.put("outputId", node.getProjectOutput().getId());
        data.put("nodeType", node.getType());
        data.put("nodeDataJson", node.getDataJson());
        data.put("positionX", node.getPositionX());
        data.put("positionY", node.getPositionY());

        // Full character data, including optional fields.
        // We compare everything so a new user edit is never
        // silently replaced by an older AI proposal.
        data.put("name", c.getName());
        data.put("shortDescription", c.getShortDescription());
        data.put("role", c.getRole());
        data.put("aliases", c.getAliases());
        data.put("age", c.getAge());
        data.put("appearance", c.getAppearance());
        data.put("traits", c.getTraits());
        data.put("strengths", c.getStrengths());
        data.put("weaknesses", c.getWeaknesses());
        data.put("skills", c.getSkills());
        data.put("history", c.getHistory());
        data.put("goals", c.getGoals());
        data.put("fears", c.getFears());
        data.put("innerConflict", c.getInnerConflict());
        data.put("secrets", c.getSecrets());
        data.put("characterValues", c.getCharacterValues());
        data.put("characterArc", c.getCharacterArc());
        data.put("writerNotes", c.getWriterNotes());

        return data;
    }


    // Copy the current character into a full DTO.
    // This prevents PATCH-like AI updates from clearing other fields.
    private CharacterProfileDTO buildCharacterDTO(ProjectNode node, CharacterProfile c) {

        CharacterProfileDTO dto = new CharacterProfileDTO();

        // Keep the existing canvas position by default.
        dto.setPositionX(node.getPositionX());
        dto.setPositionY(node.getPositionY());

        // Keep all existing character fields by default.
        dto.setName(c.getName());
        dto.setShortDescription(c.getShortDescription());
        dto.setRole(c.getRole());
        dto.setAliases(c.getAliases());
        dto.setAge(c.getAge());
        dto.setAppearance(c.getAppearance());
        dto.setTraits(c.getTraits());
        dto.setStrengths(c.getStrengths());
        dto.setWeaknesses(c.getWeaknesses());
        dto.setSkills(c.getSkills());
        dto.setHistory(c.getHistory());
        dto.setGoals(c.getGoals());
        dto.setFears(c.getFears());
        dto.setInnerConflict(c.getInnerConflict());
        dto.setSecrets(c.getSecrets());
        dto.setCharacterValues(c.getCharacterValues());
        dto.setCharacterArc(c.getCharacterArc());
        dto.setWriterNotes(c.getWriterNotes());

        return dto;
    }


    // Apply one explicitly approved field to the complete DTO.
    // The enum prevents unknown fields from being applied.
    // Null is allowed only for optional fields.
    private void applyField(CharacterProfileDTO dto, CharacterProposalField field, Object value) {

        switch (field) {

            // Canvas coordinates
            case POSITION_X ->
                    dto.setPositionX(finiteDouble(value));

            case POSITION_Y ->
                    dto.setPositionY(finiteDouble(value));

            // Required character fields
            case NAME ->
                    dto.setName(requiredText(value, 255));

            case SHORT_DESCRIPTION ->
                    dto.setShortDescription(requiredText(value, 20000));

            case ROLE ->
                    dto.setRole(requiredText(value, 50));

            case AGE ->
                    dto.setAge(positiveInteger(value));

            // Optional character fields
            case ALIASES ->
                    dto.setAliases(optionalText(value, 100));

            case APPEARANCE ->
                    dto.setAppearance(optionalText(value, 20000));

            case TRAITS ->
                    dto.setTraits(optionalText(value, 50));

            case STRENGTHS ->
                    dto.setStrengths(optionalText(value, 50));

            case WEAKNESSES ->
                    dto.setWeaknesses(optionalText(value, 50));

            case SKILLS ->
                    dto.setSkills(optionalText(value, 50));

            case HISTORY ->
                    dto.setHistory(optionalText(value, 20000));

            case GOALS ->
                    dto.setGoals(optionalText(value, 50));

            case FEARS ->
                    dto.setFears(optionalText(value, 50));

            case INNER_CONFLICT ->
                    dto.setInnerConflict(optionalText(value, 20000));

            case SECRETS ->
                    dto.setSecrets(optionalText(value, 50));

            case CHARACTER_VALUES ->
                    dto.setCharacterValues(optionalText(value, 50));

            case CHARACTER_ARC ->
                    dto.setCharacterArc(optionalText(value, 20000));

            case WRITER_NOTES ->
                    dto.setWriterNotes(optionalText(value, 20000));
        }
    }


    // Reject null, blank or oversized required strings.
    private String requiredText(Object value, int maxLength) {

        if (!(value instanceof String text) || text.isBlank() || text.length() > maxLength) {

            throw new ApiException("Invalid required character text");
        }

        return text.trim();
    }


    // Optional fields may explicitly be cleared with null.
    // If a field is omitted, it is never passed to this method.
    private String optionalText(Object value, int maxLength) {

        if (value == null) {
            return null;
        }

        if (!(value instanceof String text) || text.length() > maxLength) {

            throw new ApiException("Invalid optional character text");
        }

        return text;
    }


    // Enforce positive integers for age.
    private Integer positiveInteger(Object value) {

        if (!(value instanceof Number number)) {
            throw new ApiException("Character age must be numeric");
        }

        double n = number.doubleValue();

        if (!Double.isFinite(n) || n < 1 || n > Integer.MAX_VALUE || Math.floor(n) != n) {

            throw new ApiException("Invalid character age");
        }

        return number.intValue();
    }


    // Prevent invalid or infinite canvas coordinates.
    private Double finiteDouble(Object value) {

        if (!(value instanceof Number number)) {
            throw new ApiException("Character position must be numeric");
        }

        double n = number.doubleValue();

        if (!Double.isFinite(n)) {
            throw new ApiException("Invalid character position");
        }

        return n;
    }


    // Parse a stored JSON object into a plain string-keyed map.
    private Map<String, Object> readJson(String json) {

        if (json == null || json.isBlank()) {
            throw new ApiException("Missing character proposal data");
        }

        try {
            Map<?, ?> raw = jsonMapper.readValue(json, Map.class);

            if (raw == null) {
                throw new ApiException("Invalid character JSON");
            }

            return stringKeyMap(raw);

        } catch (JacksonException e) {
            throw new ApiException("Cannot read character proposal JSON");
        }
    }


    // Extract only the proposed field changes.
    private Map<String, Object> getProposedFields(Object value) {

        if (!(value instanceof Map<?, ?> raw)) {
            throw new ApiException("Invalid proposed character fields");
        }

        return stringKeyMap(raw);
    }


    // Reject non-string JSON keys before processing any update.
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


    // Serialize only plain map data, never Hibernate entities.
    private String toJson(Object data) {

        try {
            return jsonMapper.writeValueAsString(data);

        } catch (JacksonException e) {
            throw new ApiException(
                    "Cannot serialize character snapshot");
        }
    }


    // Internal data used only during approval.
    // This is a Java record, NOT a database entity.
    private record PreparedUpdate(
            Integer characterId,
            CharacterProfileDTO dto
    ) {}
}

//هذا الكلاس مسؤول عن ثلاث عمليات فقط:
//1. أخذ Snapshot من الشخصية عند إنشاء المقترح.
//2. التحقق من أن الشخصية لم تتغير قبل الموافقة.
//3. دمج التعديلات المعتمدة مع البيانات الحالية، ثم استخدام CharacterProfileService الموجودة.