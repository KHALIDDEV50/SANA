package com.example.sana.AI.Service;

import com.example.sana.AI.DTO.*;
import com.example.sana.AI.Enums.AIContextScope;
import com.example.sana.API.ApiException;
import com.example.sana.Repository.ProjectOutputRepository;
import com.example.sana.Model.ProjectOutput;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AIContextSelectionService {

    private final ProjectContextService projectContextService;
    private final ProjectOutputRepository projectOutputRepository;


    // Select only the project information required for an AI task
    public ProjectContextDTO selectContext(Integer projectId, Integer outputId, AIContextScope scope, Integer selectedNodeId) {

        if (projectId == null || scope == null) {
            throw new ApiException("Project ID and AI context scope are required");
        }

        // Load the project context prepared by our existing services
        ProjectContextDTO full = projectContextService.buildProjectContext(projectId);

        CreativityContextDTO creativity = full.creativity();
        GameContextDTO game = full.game();
        List<ProjectContextDTO.OutputInfo> outputInfo = full.outputs();

        // Restrict all project context categories to a selected output.
        // Node IDs are used for creativity because these DTOs do not contain output IDs.
        if (outputId != null) {
            ProjectOutput selectedOutput = projectOutputRepository.findProjectOutputById(outputId);
            if (selectedOutput == null || !projectId.equals(selectedOutput.getProject().getId())) throw new ApiException("Invalid selected output");
            Set<Integer> outputNodeIds = new HashSet<>();
            selectedOutput.getNodes().forEach(node -> outputNodeIds.add(node.getId()));
            creativity = filterCreativity(creativity, outputNodeIds);
            game = new GameContextDTO(
                    game.scenes().stream().filter(x -> outputId.equals(x.outputId())).toList(),
                    game.choices().stream().filter(x -> outputId.equals(x.outputId())).toList(),
                    game.variables().stream().filter(x -> outputId.equals(x.outputId())).toList(),
                    game.playableCharacters().stream().filter(x -> outputId.equals(x.outputId())).toList());
            outputInfo = outputInfo.stream().filter(x -> outputId.equals(x.outputId())).toList();
        }

        // Select the relevant data based on the requested AI task
        switch (scope) {
            case OVERVIEW:
                creativity = emptyCreativity();
                game = emptyGame();
                break;
            case CHARACTER:
                creativity = new CreativityContextDTO(creativity.characters(), List.of(), List.of(), List.of(), creativity.storyEvents(), creativity.relations());
                game = emptyGame();
                break;
            case WORLD_BUILDING:
                creativity = new CreativityContextDTO(List.of(), creativity.worlds(), creativity.locations(), creativity.worldRules(), List.of(), creativity.relations());
                game = emptyGame();
                break;
            case STORY_PLOT:
                game = emptyGame();
                break;
            case GAME_NARRATIVE:
                creativity = new CreativityContextDTO(creativity.characters(), List.of(), List.of(), creativity.worldRules(), creativity.storyEvents(), creativity.relations());
                game = new GameContextDTO(game.scenes(), game.choices(), game.variables(), List.of());
                break;
            case CHARACTER_BALANCE:
                creativity = new CreativityContextDTO(creativity.characters(), List.of(), List.of(), List.of(), List.of(), List.of());
                game = new GameContextDTO(List.of(), List.of(), List.of(), game.playableCharacters());
                break;
            case FULL_PROJECT:
                break;
            default:
                throw new ApiException("Unsupported AI context scope");
        }

        // Remove relationships whose elements were excluded
        Set<Integer> availableNodeIds = collectNodeIds(creativity, game);

        creativity = filterCreativity(creativity, availableNodeIds);

        // A selected node allows the AI to focus on one canvas element
        if (selectedNodeId != null) {

            if (scope == AIContextScope.OVERVIEW || scope == AIContextScope.CHARACTER_BALANCE) {
                throw new ApiException("This AI context scope does not support a selected node");
            }

            if (!availableNodeIds.contains(selectedNodeId)) {
                throw new ApiException("Selected node is not available in this project context");
            }

            // Include the selected node and its direct connections
            Set<Integer> focusedIds = findRelatedNodeIds(selectedNodeId, creativity, game);

            creativity = filterCreativity(creativity, focusedIds);
            game = filterGame(game, focusedIds);
        }

        // Return a new DTO without changing any database entity
        return new ProjectContextDTO(full.projectId(), full.projectName(), full.description(), full.domain(), full.projectStatus(), full.projectGoal(), outputInfo, creativity, game);
    }


    // Create an empty creativity context
    private CreativityContextDTO emptyCreativity() {

        return new CreativityContextDTO(List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }


    // Create an empty game context
    private GameContextDTO emptyGame() {

        return new GameContextDTO(List.of(), List.of(), List.of(), List.of());
    }


    // Collect canvas node IDs that have data in the selected context
    private Set<Integer> collectNodeIds(CreativityContextDTO c, GameContextDTO g) {

        Set<Integer> ids = new HashSet<>();

        c.characters().forEach(x -> ids.add(x.nodeId()));
        c.worlds().forEach(x -> ids.add(x.nodeId()));
        c.locations().forEach(x -> ids.add(x.nodeId()));
        c.worldRules().forEach(x -> ids.add(x.nodeId()));
        c.storyEvents().forEach(x -> ids.add(x.nodeId()));

        g.scenes().forEach(x -> ids.add(x.projectNodeId()));

        return ids;
    }


    // Keep only creativity elements and relationships inside the selection
    private CreativityContextDTO filterCreativity(CreativityContextDTO c, Set<Integer> nodeIds) {

        return new CreativityContextDTO(

                c.characters().stream()
                        .filter(x -> nodeIds.contains(x.nodeId()))
                        .toList(),

                c.worlds().stream()
                        .filter(x -> nodeIds.contains(x.nodeId()))
                        .toList(),

                c.locations().stream()
                        .filter(x -> nodeIds.contains(x.nodeId()))
                        .toList(),

                c.worldRules().stream()
                        .filter(x -> nodeIds.contains(x.nodeId()))
                        .toList(),

                c.storyEvents().stream()
                        .filter(x -> nodeIds.contains(x.nodeId()))
                        .toList(),

                // Exclude relationships with missing endpoints
                c.relations().stream()
                        .filter(r ->
                                nodeIds.contains(r.sourceNodeId())
                                        && nodeIds.contains(r.targetNodeId()))
                        .toList()
        );
    }


    // Find the selected node and its directly connected elements
    private Set<Integer> findRelatedNodeIds(Integer selectedNodeId, CreativityContextDTO creativity, GameContextDTO game) {

        Set<Integer> ids = new HashSet<>();
        ids.add(selectedNodeId);

        // Include neighboring elements connected by creativity edges
        for (CreativityContextDTO.RelationInfo relation : creativity.relations()) {

            if (selectedNodeId.equals(relation.sourceNodeId())) {
                ids.add(relation.targetNodeId());
            }

            if (selectedNodeId.equals(relation.targetNodeId())) {
                ids.add(relation.sourceNodeId());
            }
        }

        // Map game scene IDs to their visual ProjectNode IDs
        Map<Integer, Integer> sceneNodeIds = new HashMap<>();

        for (GameContextDTO.SceneInfo scene : game.scenes()) {
            sceneNodeIds.put(scene.id(), scene.projectNodeId());
        }

        // If a game scene is selected, include its connected scenes
        for (GameContextDTO.ChoiceInfo choice : game.choices()) {

            Integer sourceNodeId = sceneNodeIds.get(choice.sourceSceneId());

            Integer targetNodeId = sceneNodeIds.get(choice.targetSceneId());

            if (selectedNodeId.equals(sourceNodeId) && targetNodeId != null) {
                ids.add(targetNodeId);
            }

            if (selectedNodeId.equals(targetNodeId) && sourceNodeId != null) {
                ids.add(sourceNodeId);
            }
        }

        // Include story references for selected game scenes
        for (GameContextDTO.SceneInfo scene : game.scenes()) {

            if (!ids.contains(scene.projectNodeId())) {
                continue;
            }

            creativity.characters().stream().filter(c -> c.id().equals(scene.speakerCharacterId())).forEach(c -> ids.add(c.nodeId()));

            creativity.storyEvents().stream().filter(e -> e.id().equals(scene.relatedStoryEventId())).forEach(e -> ids.add(e.nodeId()));
        }

        return ids;
    }


    // Keep only the game scenes and choices inside the focused selection
    private GameContextDTO filterGame(GameContextDTO game, Set<Integer> nodeIds) {

        List<GameContextDTO.SceneInfo> scenes = game.scenes().stream().filter(s -> nodeIds.contains(s.projectNodeId())).toList();

        Set<Integer> sceneIds = new HashSet<>();

        scenes.forEach(scene -> sceneIds.add(scene.id()));

        List<GameContextDTO.ChoiceInfo> choices = game.choices().stream().filter(choice -> sceneIds.contains(choice.sourceSceneId()) && sceneIds.contains(choice.targetSceneId())).toList();

        // Include variables referenced by the selected choices
        List<GameContextDTO.VariableInfo> variables = game.variables().stream().filter(variable -> choices.stream().anyMatch(choice -> variable.outputId().equals(choice.outputId()) && (
                variable.name().equals(choice.conditionVariable()) || variable.name().equals(choice.effectVariable())))).toList();

        Set<Integer> selectedOutputIds = new HashSet<>();
        scenes.forEach(scene -> selectedOutputIds.add(scene.outputId()));
        return new GameContextDTO(scenes, choices, variables, game.playableCharacters().stream().filter(x -> selectedOutputIds.contains(x.outputId())).toList());
    }
}
