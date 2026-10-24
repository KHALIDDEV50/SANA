package com.example.sana.AI.Service;

import com.example.sana.AI.DTO.AIProposalDraftDTO;
import com.example.sana.API.ApiException;
import com.example.sana.DTO.CreativityDomainDTOs.*;
import com.example.sana.Enums.ChangeOperation;
import com.example.sana.Enums.ChangeTargetType;
import com.example.sana.Enums.CreativityDomainEnums.BranchingNodeType;
import com.example.sana.Model.*;
import com.example.sana.Model.CreativityDomainModels.*;
import com.example.sana.Repository.ProjectOutputRepository;
import com.example.sana.Repository.ProjectEdgeRepository;
import com.example.sana.Repository.ProjectNodeRepository;
import com.example.sana.Repository.CreativityDomainRepository.*;
import com.example.sana.Service.CreativityDomainServise.BranchingNarrativeService;
import com.example.sana.Service.CreativityDomainServise.CharacterBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AIGameProposalService {

    private final BranchingNarrativeService branchingNarrativeService;
    private final CharacterBalanceService characterBalanceService;
    private final ProjectOutputRepository projectOutputRepository;
    private final ProjectNodeRepository projectNodeRepository;
    private final ProjectEdgeRepository projectEdgeRepository;
    private final BranchingNodeRepository branchingNodeRepository;
    private final BranchingChoiceRepository branchingChoiceRepository;
    private final BranchingVariableRepository branchingVariableRepository;
    private final PlayableCharacterRepository playableCharacterRepository;
    private final JsonMapper jsonMapper;

    // The existing ChangeTargetType has NODE, EDGE and RECORD only.
    // Game variables and playable characters use RECORD as a non-canvas target.
    // Their elementType is mandatory and prevents collisions with ProjectRecord.
    private static final Set<String> SCENE_FIELDS = Set.of("title", "content", "type", "positionX", "positionY");
    private static final Set<String> CHOICE_CREATE_FIELDS = Set.of("text", "displayOrder", "sourceSceneId", "targetSceneId");
    private static final Set<String> CHOICE_UPDATE_FIELDS = Set.of("text", "displayOrder", "targetSceneId");
    private static final Set<String> VARIABLE_FIELDS = Set.of("name", "initialValue");
    private static final Set<String> PLAYABLE_FIELDS = Set.of("name", "role", "weaponName", "weaponType", "strengths", "weaknesses", "attack", "defense", "speed", "rangeStat", "stamina", "special");

    public boolean supports(String elementType) {
        return elementType != null && Set.of("BRANCHING_SCENE", "GAME_CHOICE", "GAME_VARIABLE", "PLAYABLE_CHARACTER").contains(elementType);
    }

    // Check AI data before saving a PENDING proposal; do not write any game data.
    @Transactional(readOnly = true)
    public void validateDraft(Integer projectId, AIProposalDraftDTO.ProposedItem item) {
        if (item == null || !supports(item.elementType()) || item.operation() == ChangeOperation.DELETE) {
            throw new ApiException("Unsupported game proposal");
        }
        validateTargetType(item.targetType(), item.elementType());
        requireOutput(projectId, item.outputId());
        if (item.operation() == ChangeOperation.CREATE && item.targetId() != null) throw new ApiException("CREATE cannot specify a target ID");
        if (item.operation() == ChangeOperation.UPDATE && (item.targetId() == null || item.targetId() <= 0)) throw new ApiException("UPDATE requires target ID");
        Map<String, Object> fields = requireFields(item.proposedFields());
        switch (item.elementType()) {
            case "BRANCHING_SCENE":
                checkAllowed(fields, SCENE_FIELDS);
                if (item.operation() == ChangeOperation.UPDATE) getScene(item.outputId(), item.targetId());
                prepareScene(fields, item.operation() == ChangeOperation.UPDATE ? getScene(item.outputId(), item.targetId()) : null);
                break;
            case "GAME_CHOICE":
                checkAllowed(fields, item.operation() == ChangeOperation.CREATE ? CHOICE_CREATE_FIELDS : CHOICE_UPDATE_FIELDS);
                BranchingChoice choice = item.operation() == ChangeOperation.UPDATE ? getChoice(item.outputId(), item.targetId()) : null;
                prepareChoice(fields, choice, item.outputId());
                break;
            case "GAME_VARIABLE":
                checkAllowed(fields, VARIABLE_FIELDS);
                prepareVariable(fields, item.operation() == ChangeOperation.UPDATE ? getVariable(item.outputId(), item.targetId()) : null);
                break;
            case "PLAYABLE_CHARACTER":
                checkAllowed(fields, PLAYABLE_FIELDS);
                preparePlayable(fields, item.operation() == ChangeOperation.UPDATE ? getPlayable(item.outputId(), item.targetId()) : null);
                break;
            default:
                throw new ApiException("Unsupported game element");
        }
    }

    // Store ALL relevant details, not just ProjectNode/ProjectEdge metadata.
    @Transactional(readOnly = true)
    public String createBeforeSnapshot(Integer projectId, AIProposalDraftDTO.ProposedItem item) {
        validateDraft(projectId, item);
        if (item.operation() != ChangeOperation.UPDATE) throw new ApiException("Snapshot requires UPDATE");
        return toJson(snapshot(item.outputId(), item.targetId(), item.elementType()));
    }

    // Validate saved proposals again inside the approval transaction.
    public void validateSaved(Integer projectId, ChangeProposalItem item) {
        SavedGameChange change = readSaved(item);
        AIProposalDraftDTO.ProposedItem draft = new AIProposalDraftDTO.ProposedItem(item.getOperation(), item.getTargetType(), item.getTargetId(), change.outputId(), change.elementType(), change.fields());
        validateDraft(projectId, draft);
        if (item.getOperation() == ChangeOperation.UPDATE) {
            Map<String, Object> current = snapshot(change.outputId(), item.getTargetId(), change.elementType());
            if (!readJson(item.getBeforeJson()).equals(current)) throw new ApiException("Game element changed after the AI proposal was created");
        } else if (item.getBeforeJson() != null) {
            throw new ApiException("CREATE cannot have a previous snapshot");
        }
    }

    // Apply through the team's services, inside the caller's approval transaction.
    public void applySaved(Integer projectId, ChangeProposalItem item) {
        validateSaved(projectId, item);
        SavedGameChange change = readSaved(item);
        Integer outputId = change.outputId();
        Map<String, Object> fields = change.fields();
        switch (change.elementType()) {
            case "BRANCHING_SCENE":
                BranchingNode scene = item.getOperation() == ChangeOperation.UPDATE ? getScene(outputId, item.getTargetId()) : null;
                BranchingNodeDTO sceneDTO = prepareScene(fields, scene);
                if (scene == null) branchingNarrativeService.addNode(outputId, sceneDTO);
                else branchingNarrativeService.updateNode(outputId, scene.getId(), sceneDTO);
                break;
            case "GAME_CHOICE":
                BranchingChoice choice = item.getOperation() == ChangeOperation.UPDATE ? getChoice(outputId, item.getTargetId()) : null;
                BranchingChoiceDTO choiceDTO = prepareChoice(fields, choice, outputId);
                if (choice == null) branchingNarrativeService.addChoice(outputId, intValue(fields.get("sourceSceneId"), false), intValue(fields.get("targetSceneId"), false), choiceDTO);
                else branchingNarrativeService.updateChoice(outputId, choice.getId(), fields.containsKey("targetSceneId") ? intValue(fields.get("targetSceneId"), false) : sceneIdForProjectNode(choice.getProjectEdge().getTargetNode().getId(), outputId), choiceDTO);
                break;
            case "GAME_VARIABLE":
                BranchingVariable variable = item.getOperation() == ChangeOperation.UPDATE ? getVariable(outputId, item.getTargetId()) : null;
                BranchingVariableDTO variableDTO = prepareVariable(fields, variable);
                if (variable == null) branchingNarrativeService.addVariable(outputId, variableDTO);
                else branchingNarrativeService.updateVariable(outputId, variable.getId(), variableDTO);
                break;
            case "PLAYABLE_CHARACTER":
                PlayableCharacter playable = item.getOperation() == ChangeOperation.UPDATE ? getPlayable(outputId, item.getTargetId()) : null;
                PlayableCharacterDTO playableDTO = preparePlayable(fields, playable);
                if (playable == null) characterBalanceService.addCharacter(outputId, playableDTO);
                else characterBalanceService.updateCharacter(outputId, playable.getId(), playableDTO);
                break;
            default:
                throw new ApiException("Unsupported game proposal");
        }
    }

    private void validateTargetType(ChangeTargetType targetType, String elementType) {
        ChangeTargetType expected;
        switch (elementType) {
            case "BRANCHING_SCENE": expected = ChangeTargetType.NODE; break;
            case "GAME_CHOICE": expected = ChangeTargetType.EDGE; break;
            case "GAME_VARIABLE":
            case "PLAYABLE_CHARACTER": expected = ChangeTargetType.RECORD; break;
            default: throw new ApiException("Unsupported game element type");
        }
        if (targetType != expected) throw new ApiException("Incorrect target type for " + elementType);
    }

    private SavedGameChange readSaved(ChangeProposalItem item) {
        if (item == null || item.getOperation() == null || item.getTargetType() == null || item.getOperation() == ChangeOperation.DELETE) throw new ApiException("Unsupported game operation");
        Map<String, Object> after = readJson(item.getAfterJson());
        if (!after.keySet().equals(Set.of("outputId", "elementType", "fields"))) throw new ApiException("Invalid game proposal format");
        if (!(after.get("elementType") instanceof String elementType) || !supports(elementType)) throw new ApiException("Invalid game elementType");
        validateTargetType(item.getTargetType(), elementType);
        Integer outputId = intValue(after.get("outputId"), false);
        return new SavedGameChange(outputId, elementType, requireFields(after.get("fields")));
    }

    private void requireOutput(Integer projectId, Integer outputId) {
        if (outputId == null || projectId == null) throw new ApiException("Project and output are required");
        ProjectOutput output = projectOutputRepository.findById(outputId).orElseThrow(() -> new ApiException("Output not found"));
        if (output.getProject() == null || !projectId.equals(output.getProject().getId())) throw new ApiException("Output does not belong to the project");
    }

    private BranchingNode getScene(Integer outputId, Integer projectNodeId) {
        ProjectNode node = projectNodeRepository.findById(projectNodeId).orElseThrow(() -> new ApiException("Scene node not found"));
        if (!outputId.equals(node.getProjectOutput().getId()) || !"BRANCHING".equals(node.getType())) throw new ApiException("Invalid scene target");
        BranchingNode scene = branchingNodeRepository.findByProjectNode_Id(projectNodeId);
        if (scene == null) throw new ApiException("Branching scene not found");
        return scene;
    }

    private Integer sceneIdForProjectNode(Integer projectNodeId, Integer outputId) {
        return getScene(outputId, projectNodeId).getId();
    }

    private void requireSceneId(Integer outputId, Integer sceneId) {
        if (branchingNodeRepository.findByIdAndProjectNode_ProjectOutput_Id(sceneId, outputId) == null) throw new ApiException("Scene is not in the output");
    }

    private BranchingChoice getChoice(Integer outputId, Integer projectEdgeId) {
        ProjectEdge edge = projectEdgeRepository.findById(projectEdgeId).orElseThrow(() -> new ApiException("Choice edge not found"));
        if (!outputId.equals(edge.getProjectOutput().getId()) || !"BRANCH_CHOICE".equals(edge.getType())) throw new ApiException("Invalid choice edge");
        return branchingChoiceRepository.findByProjectEdge_ProjectOutput_IdOrderByIdAsc(outputId).stream()
                .filter(choice -> projectEdgeId.equals(choice.getProjectEdge().getId())).findFirst()
                .orElseThrow(() -> new ApiException("Choice not found"));
    }

    private BranchingVariable getVariable(Integer outputId, Integer variableId) {
        BranchingVariable variable = branchingVariableRepository.findByIdAndProjectOutput_Id(variableId, outputId);
        if (variable == null) throw new ApiException("Variable not found in output");
        return variable;
    }

    private PlayableCharacter getPlayable(Integer outputId, Integer playableId) {
        PlayableCharacter playable = playableCharacterRepository.findByIdAndProjectOutput_Id(playableId, outputId);
        if (playable == null) throw new ApiException("Playable character not found in output");
        return playable;
    }

    private BranchingNodeDTO prepareScene(Map<String, Object> fields, BranchingNode scene) {
        BranchingNodeDTO dto = new BranchingNodeDTO();
        dto.setTitle(fields.containsKey("title") ? text(fields.get("title"), 255, true) : scene == null ? text(null, 255, true) : scene.getTitle());
        dto.setContent(fields.containsKey("content") ? text(fields.get("content"), 16000, true) : scene == null ? text(null, 16000, true) : scene.getContent());
        String type = fields.containsKey("type") ? text(fields.get("type"), 30, true) : scene == null ? null : scene.getType().name();
        try { dto.setType(BranchingNodeType.valueOf(type)); } catch (RuntimeException e) { throw new ApiException("Invalid branching scene type"); }
        dto.setPositionX(fields.containsKey("positionX") ? coordinate(fields.get("positionX")) : scene == null ? coordinate(null) : scene.getProjectNode().getPositionX());
        dto.setPositionY(fields.containsKey("positionY") ? coordinate(fields.get("positionY")) : scene == null ? coordinate(null) : scene.getProjectNode().getPositionY());
        return dto;
    }

    private BranchingChoiceDTO prepareChoice(Map<String, Object> fields, BranchingChoice choice, Integer outputId) {
        BranchingChoiceDTO dto = new BranchingChoiceDTO();
        dto.setText(fields.containsKey("text") ? text(fields.get("text"), 16000, true) : choice == null ? text(null, 16000, true) : choice.getText());
        dto.setDisplayOrder(fields.containsKey("displayOrder") ? intValue(fields.get("displayOrder"), true) : choice == null ? intValue(null, true) : choice.getDisplayOrder());
        if (dto.getDisplayOrder() < 0) throw new ApiException("displayOrder must be non-negative");
        if (choice == null) {
            requireSceneId(outputId, intValue(fields.get("sourceSceneId"), false));
            requireSceneId(outputId, intValue(fields.get("targetSceneId"), false));
        } else if (fields.containsKey("targetSceneId")) {
            requireSceneId(outputId, intValue(fields.get("targetSceneId"), false));
        }
        return dto;
    }

    private BranchingVariableDTO prepareVariable(Map<String, Object> fields, BranchingVariable existing) {
        BranchingVariableDTO dto = new BranchingVariableDTO();
        String name = fields.containsKey("name") ? text(fields.get("name"), 40, true) : existing == null ? text(null, 40, true) : existing.getName();
        if (!name.matches("[A-Za-z][A-Za-z0-9_]{0,39}")) throw new ApiException("Invalid variable name");
        dto.setName(name);
        dto.setInitialValue(fields.containsKey("initialValue") ? intValue(fields.get("initialValue"), true) : existing == null ? intValue(null, true) : existing.getInitialValue());
        return dto;
    }

    private PlayableCharacterDTO preparePlayable(Map<String, Object> fields, PlayableCharacter existing) {
        PlayableCharacterDTO dto = new PlayableCharacterDTO();
        dto.setName(stringField(fields, "name", existing == null ? null : existing.getName(), 100, true));
        dto.setRole(stringField(fields, "role", existing == null ? null : existing.getRole(), 100, false));
        dto.setWeaponName(stringField(fields, "weaponName", existing == null ? null : existing.getWeaponName(), 100, false));
        dto.setWeaponType(stringField(fields, "weaponType", existing == null ? null : existing.getWeaponType(), 100, false));
        dto.setStrengths(stringField(fields, "strengths", existing == null ? null : existing.getStrengths(), 16000, false));
        dto.setWeaknesses(stringField(fields, "weaknesses", existing == null ? null : existing.getWeaknesses(), 16000, false));
        dto.setAttack(statField(fields, "attack", existing == null ? null : existing.getAttack()));
        dto.setDefense(statField(fields, "defense", existing == null ? null : existing.getDefense()));
        dto.setSpeed(statField(fields, "speed", existing == null ? null : existing.getSpeed()));
        dto.setRangeStat(statField(fields, "rangeStat", existing == null ? null : existing.getRangeStat()));
        dto.setStamina(statField(fields, "stamina", existing == null ? null : existing.getStamina()));
        dto.setSpecial(statField(fields, "special", existing == null ? null : existing.getSpecial()));
        return dto;
    }

    private String stringField(Map<String, Object> fields, String key, String current, int max, boolean required) {
        return fields.containsKey(key) ? text(fields.get(key), max, required) : required && current == null ? text(null, max, true) : current;
    }

    private Integer statField(Map<String, Object> fields, String key, Integer current) {
        if (!fields.containsKey(key)) return current;
        if (fields.get(key) == null) return null;
        Integer stat = intValue(fields.get(key), true);
        if (stat < 0 || stat > 10) throw new ApiException("Game stat must be between 0 and 10");
        return stat;
    }

    // Snapshots contain the complete persisted data (including relationships).
    private Map<String, Object> snapshot(Integer outputId, Integer id, String elementType) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("snapshotType", "SANA_GAME_V1");
        data.put("outputId", outputId);
        data.put("elementType", elementType);
        switch (elementType) {
            case "BRANCHING_SCENE":
                BranchingNode scene = getScene(outputId, id);
                data.put("nodeId", id); data.put("sceneId", scene.getId());
                data.put("title", scene.getTitle()); data.put("content", scene.getContent()); data.put("type", scene.getType().name());
                data.put("positionX", scene.getProjectNode().getPositionX()); data.put("positionY", scene.getProjectNode().getPositionY());
                data.put("speakerId", scene.getSpeaker() == null ? null : scene.getSpeaker().getId());
                data.put("eventId", scene.getRelatedStoryEvent() == null ? null : scene.getRelatedStoryEvent().getId());
                break;
            case "GAME_CHOICE":
                BranchingChoice choice = getChoice(outputId, id);
                data.put("edgeId", id); data.put("choiceId", choice.getId());
                data.put("sourceNodeId", choice.getProjectEdge().getSourceNode().getId());
                data.put("targetNodeId", choice.getProjectEdge().getTargetNode().getId());
                data.put("text", choice.getText()); data.put("displayOrder", choice.getDisplayOrder());
                data.put("conditionVariableId", choice.getConditionVariable() == null ? null : choice.getConditionVariable().getId());
                data.put("conditionOperator", choice.getConditionOperator() == null ? null : choice.getConditionOperator().name());
                data.put("conditionValue", choice.getConditionValue());
                data.put("effectVariableId", choice.getEffectVariable() == null ? null : choice.getEffectVariable().getId());
                data.put("effectOperator", choice.getEffectOperator() == null ? null : choice.getEffectOperator().name());
                data.put("effectValue", choice.getEffectValue());
                break;
            case "GAME_VARIABLE":
                BranchingVariable variable = getVariable(outputId, id);
                data.put("variableId", id); data.put("name", variable.getName()); data.put("initialValue", variable.getInitialValue());
                break;
            case "PLAYABLE_CHARACTER":
                PlayableCharacter playable = getPlayable(outputId, id);
                data.put("playableId", id); data.put("name", playable.getName()); data.put("role", playable.getRole());
                data.put("weaponName", playable.getWeaponName()); data.put("weaponType", playable.getWeaponType());
                data.put("strengths", playable.getStrengths()); data.put("weaknesses", playable.getWeaknesses());
                data.put("attack", playable.getAttack()); data.put("defense", playable.getDefense()); data.put("speed", playable.getSpeed());
                data.put("rangeStat", playable.getRangeStat()); data.put("stamina", playable.getStamina()); data.put("special", playable.getSpecial());
                data.put("characterProfileId", playable.getCharacterProfile() == null ? null : playable.getCharacterProfile().getId());
                break;
            default: throw new ApiException("Unsupported game snapshot");
        }
        return data;
    }

    private Map<String, Object> readJson(String json) {
        if (json == null || json.isBlank()) throw new ApiException("Missing game proposal JSON");
        try {
            Map<?, ?> raw = jsonMapper.readValue(json, Map.class);
            if (raw == null) throw new ApiException("Invalid game JSON");
            return asMap(raw);
        } catch (JacksonException e) { throw new ApiException("Cannot parse game proposal JSON"); }
    }

    private String toJson(Map<String, Object> value) {
        try { return jsonMapper.writeValueAsString(value); }
        catch (JacksonException e) { throw new ApiException("Cannot save game snapshot"); }
    }

    private Map<String, Object> requireFields(Object value) {
        if (!(value instanceof Map<?, ?> map)) throw new ApiException("Game fields must be an object");
        Map<String, Object> fields = asMap(map);
        if (fields.isEmpty()) throw new ApiException("Game fields cannot be empty");
        return fields;
    }

    private Map<String, Object> asMap(Map<?, ?> raw) {
        Map<String, Object> data = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (!(entry.getKey() instanceof String key)) throw new ApiException("Invalid game field name");
            data.put(key, entry.getValue());
        }
        return data;
    }

    private void checkAllowed(Map<String, Object> fields, Set<String> allowed) {
        if (!allowed.containsAll(fields.keySet())) throw new ApiException("Unsupported game field");
    }

    private String text(Object value, int max, boolean required) {
        if (value == null && !required) return null;
        if (!(value instanceof String s) || s.length() > max || (required && s.isBlank())) throw new ApiException("Invalid game text field");
        return s;
    }

    private Integer intValue(Object value, boolean allowZeroOrNegative) {
        if (!(value instanceof Number number)) throw new ApiException("Integer value required");
        double n = number.doubleValue();
        if (!Double.isFinite(n) || n < Integer.MIN_VALUE || n > Integer.MAX_VALUE || Math.floor(n) != n || (!allowZeroOrNegative && n <= 0)) throw new ApiException("Invalid integer value");
        return number.intValue();
    }

    private Double coordinate(Object value) {
        if (!(value instanceof Number number) || !Double.isFinite(number.doubleValue())) throw new ApiException("Valid canvas coordinate required");
        return number.doubleValue();
    }

    private record SavedGameChange(Integer outputId, String elementType, Map<String, Object> fields) {}
}
