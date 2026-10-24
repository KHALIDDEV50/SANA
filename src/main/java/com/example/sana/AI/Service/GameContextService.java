package com.example.sana.AI.Service;

import com.example.sana.AI.DTO.GameContextDTO;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Model.ProjectEdge;

import com.example.sana.Model.CreativityDomainModels.*;
import com.example.sana.Repository.CreativityDomainRepository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GameContextService {

    private final BranchingNodeRepository branchingNodeRepository;
    private final BranchingChoiceRepository branchingChoiceRepository;
    private final BranchingVariableRepository branchingVariableRepository;
    private final PlayableCharacterRepository playableCharacterRepository;


    // Collect game-related information across all project outputs
    @Transactional(readOnly = true)
    public GameContextDTO buildGameContext(List<ProjectOutput> outputs) {

        List<GameContextDTO.SceneInfo> scenes = new ArrayList<>();
        List<GameContextDTO.ChoiceInfo> choices = new ArrayList<>();
        List<GameContextDTO.VariableInfo> variables = new ArrayList<>();
        List<GameContextDTO.PlayableCharacterInfo> playableCharacters = new ArrayList<>();

        // Process every output in a stable order
        for (ProjectOutput output : outputs.stream().sorted(Comparator.comparing(ProjectOutput::getId)).toList()) {

            Integer outputId = output.getId();

            // Map visual ProjectNode IDs to BranchingNode IDs
            Map<Integer, Integer> sceneIdsByProjectNode = new HashMap<>();


            // 1. Read branching narrative scenes
            List<BranchingNode> branchingNodes = branchingNodeRepository.findByProjectNode_ProjectOutput_IdOrderByIdAsc(outputId);

            for (BranchingNode scene : branchingNodes) {

                Integer projectNodeId = scene.getProjectNode().getId();

                sceneIdsByProjectNode.put(projectNodeId, scene.getId());

                scenes.add(new GameContextDTO.SceneInfo(scene.getId(), projectNodeId, outputId, scene.getTitle(), scene.getContent(), scene.getType().name(), scene.getSpeaker() == null ? null : scene.getSpeaker().getId(), scene.getRelatedStoryEvent() == null ? null : scene.getRelatedStoryEvent().getId()));
            }


            // 2. Read player choices and their conditions/effects
            List<BranchingChoice> branchingChoices = branchingChoiceRepository.findByProjectEdge_ProjectOutput_IdOrderByIdAsc(outputId);

            for (BranchingChoice choice : branchingChoices) {

                ProjectEdge edge = choice.getProjectEdge();

                Integer sourceProjectNodeId = edge.getSourceNode().getId();

                Integer targetProjectNodeId = edge.getTargetNode().getId();

                BranchingVariable conditionVariable = choice.getConditionVariable();

                BranchingVariable effectVariable = choice.getEffectVariable();

                choices.add(new GameContextDTO.ChoiceInfo(choice.getId(), edge.getId(), outputId, sceneIdsByProjectNode.get(sourceProjectNodeId), sceneIdsByProjectNode.get(targetProjectNodeId), choice.getText(), choice.getDisplayOrder(),

                                // Optional condition
                                conditionVariable == null ? null : conditionVariable.getName(),

                                choice.getConditionOperator() == null ? null : choice.getConditionOperator().name(),

                                choice.getConditionValue(),

                                // Optional effect
                                effectVariable == null ? null : effectVariable.getName(),

                                choice.getEffectOperator() == null ? null : choice.getEffectOperator().name(),

                                choice.getEffectValue()));
            }


            // 3. Read game variables and their initial values
            List<BranchingVariable> gameVariables = branchingVariableRepository.findByProjectOutput_IdOrderByIdAsc(outputId);

            for (BranchingVariable variable : gameVariables) {

                variables.add(new GameContextDTO.VariableInfo(variable.getId(), outputId, variable.getName(), variable.getInitialValue()));
            }


            // 4. Read playable characters and optional stats
            List<PlayableCharacter> characters = playableCharacterRepository.findByProjectOutput_IdOrderByIdAsc(outputId);

            for (PlayableCharacter character : characters) {

                playableCharacters.add(new GameContextDTO.PlayableCharacterInfo(character.getId(), outputId,

                                character.getCharacterProfile() == null
                                        ? null
                                        : character.getCharacterProfile().getId(),

                                character.getName(),
                                character.getRole(),
                                character.getWeaponName(),
                                character.getWeaponType(),
                                character.getStrengths(),
                                character.getWeaknesses(),

                                character.getAttack(),
                                character.getDefense(),
                                character.getSpeed(),
                                character.getRangeStat(),
                                character.getStamina(),
                                character.getSpecial()));
            }
        }


        // Return game data without changing the database
        return new GameContextDTO(scenes, choices, variables, playableCharacters);
    }
}
