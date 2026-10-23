package com.example.sana.AI.Service;

import com.example.sana.API.ApiException;
import com.example.sana.DTO.CreativityDomainDTOs.CharacterProfileDTO;

import com.example.sana.Enums.ChangeOperation;
import com.example.sana.Enums.ChangeTargetType;
import com.example.sana.AI.Enums.CharacterProposalField;

import com.example.sana.Model.ChangeProposalItem;
import com.example.sana.Model.ProjectOutput;

import com.example.sana.Repository.ProjectOutputRepository;
import com.example.sana.Service.CreativityDomainServise.CharacterProfileService;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AICharacterCreationService {

    private final CharacterProfileService characterProfileService;
    private final ProjectOutputRepository projectOutputRepository;
    private final JsonMapper jsonMapper;
    private final Validator validator;


    // Validate a character creation proposal before applying changes
    public void validateCharacterCreate(Integer projectId, ChangeProposalItem item) {

        prepareCharacter(projectId, item);
    }


    // Create a character only after the user approves the proposal
    // The calling approval service owns the transaction
    public void createCharacter(Integer projectId, ChangeProposalItem item) {

        PreparedCharacter prepared = prepareCharacter(projectId, item);

        // Reuse the existing Creativity Domain service
        characterProfileService.addCharacterProfile(prepared.outputId(), prepared.dto());
    }


    // Convert stored proposal data into a validated CharacterProfileDTO
    private PreparedCharacter prepareCharacter(Integer projectId, ChangeProposalItem item) {

        if (projectId == null || projectId <= 0 || item == null) {
            throw new ApiException("Invalid character creation request");
        }

        if (item.getOperation() != ChangeOperation.CREATE || item.getTargetType() != ChangeTargetType.NODE || item.getTargetId() != null || item.getBeforeJson() != null) {

            throw new ApiException("Only new character nodes are supported");
        }

        Map<String, Object> after = readJson(item.getAfterJson());

        if (!after.keySet().equals(Set.of("outputId", "elementType", "fields"))) {

            throw new ApiException("Invalid character proposal structure");
        }

        if (!"CHARACTER".equals(after.get("elementType"))) {
            throw new ApiException("This handler only supports CHARACTER");
        }

        Integer outputId = positiveInteger(after.get("outputId"));

        ProjectOutput output = projectOutputRepository.findProjectOutputById(outputId);

        if (output == null || !projectId.equals(output.getProject().getId())) {

            throw new ApiException("Output does not belong to the selected project");
        }

        Map<String, Object> fields = getFields(after.get("fields"));


        // Validate all proposed fields using the enum
        if (fields.isEmpty() || !fields.keySet().stream().allMatch(CharacterProposalField::isAllowed)) {

            throw new ApiException("Character proposal contains unsupported fields");
        }


        CharacterProfileDTO dto = new CharacterProfileDTO();

        // Required character information
        dto.setName(requiredText(fields.get("name"), 255));
        dto.setShortDescription(requiredText(fields.get("shortDescription"), 20000));
        dto.setRole(requiredText(fields.get("role"), 50));
        dto.setAge(positiveInteger(fields.get("age")));

        // Required canvas position
        dto.setPositionX(finiteDouble(fields.get("positionX")));
        dto.setPositionY(finiteDouble(fields.get("positionY")));

        // Optional character details
        dto.setAliases(optionalText(fields.get("aliases"), 100));
        dto.setAppearance(optionalText(fields.get("appearance"), 20000));
        dto.setTraits(optionalText(fields.get("traits"), 50));
        dto.setStrengths(optionalText(fields.get("strengths"), 50));
        dto.setWeaknesses(optionalText(fields.get("weaknesses"), 50));
        dto.setSkills(optionalText(fields.get("skills"), 50));
        dto.setHistory(optionalText(fields.get("history"), 20000));
        dto.setGoals(optionalText(fields.get("goals"), 50));
        dto.setFears(optionalText(fields.get("fears"), 50));
        dto.setInnerConflict(optionalText(fields.get("innerConflict"), 20000));
        dto.setSecrets(optionalText(fields.get("secrets"), 50));
        dto.setCharacterValues(optionalText(fields.get("characterValues"), 50));
        dto.setCharacterArc(optionalText(fields.get("characterArc"), 20000));
        dto.setWriterNotes(optionalText(fields.get("writerNotes"), 20000));

        // Apply the existing DTO's Jakarta Validation rules
        Set<ConstraintViolation<CharacterProfileDTO>> violations = validator.validate(dto);

        if (!violations.isEmpty()) {
            throw new ApiException("Invalid character data: " + violations.iterator().next().getMessage());
        }

        return new PreparedCharacter(outputId, dto);
    }


    // Read structured proposal JSON without mapping it to a JPA Entity
    private Map<String, Object> readJson(String json) {

        if (json == null || json.isBlank()) {
            throw new ApiException("Missing character proposal data");
        }

        try {
            Map<?, ?> raw = jsonMapper.readValue(json, Map.class);

            if (raw == null) {
                throw new ApiException("Invalid proposal JSON");
            }

            return toStringKeyMap(raw);

        } catch (JacksonException e) {
            throw new ApiException("Cannot read character proposal JSON");
        }
    }


    private Map<String, Object> getFields(Object value) {

        if (!(value instanceof Map<?, ?> raw)) {
            throw new ApiException("Invalid character fields");
        }

        return toStringKeyMap(raw);
    }


    private Map<String, Object> toStringKeyMap(Map<?, ?> raw) {

        Map<String, Object> result = new HashMap<>();

        for (Map.Entry<?, ?> entry : raw.entrySet()) {

            if (!(entry.getKey() instanceof String key)) {
                throw new ApiException("Invalid JSON field name");
            }

            result.put(key, entry.getValue());
        }

        return result;
    }


    private String requiredText(Object value, int maxLength) {

        if (!(value instanceof String text) || text.isBlank() || text.length() > maxLength) {

            throw new ApiException("Required character field is missing or invalid");
        }

        return text.trim();
    }


    private String optionalText(Object value, int maxLength) {

        if (value == null) {
            return null;
        }

        if (!(value instanceof String text) || text.length() > maxLength) {

            throw new ApiException("Invalid optional character field");
        }

        return text;
    }


    private Integer positiveInteger(Object value) {

        if (!(value instanceof Number number)) {
            throw new ApiException("Positive integer is required");
        }

        double numberValue = number.doubleValue();

        if (!Double.isFinite(numberValue) || numberValue < 1 || numberValue > Integer.MAX_VALUE || Math.floor(numberValue) != numberValue) {
            throw new ApiException("Invalid positive integer");
        }

        return number.intValue();
    }


    private Double finiteDouble(Object value) {

        if (!(value instanceof Number number)) {
            throw new ApiException("Numeric position is required");
        }

        double result = number.doubleValue();

        if (!Double.isFinite(result)) {
            throw new ApiException("Invalid character position");
        }

        return result;
    }


    // Internal validated data, not a database entity
    private record PreparedCharacter(Integer outputId, CharacterProfileDTO dto
    ) {}
}
