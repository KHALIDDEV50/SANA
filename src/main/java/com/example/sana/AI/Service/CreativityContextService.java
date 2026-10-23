package com.example.sana.AI.Service;

import com.example.sana.AI.DTO.CreativityContextDTO;
import com.example.sana.Model.ProjectEdge;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Model.CreativityDomainModels.*;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CreativityContextService {

    // Collect creative elements across all outputs of one project
    // The caller must validate project access before calling this method
    public CreativityContextDTO buildCreativityContext(List<ProjectOutput> outputs) {

        List<CreativityContextDTO.CharacterInfo> characters = new ArrayList<>();

        List<CreativityContextDTO.WorldInfo> worlds = new ArrayList<>();

        List<CreativityContextDTO.LocationInfo> locations = new ArrayList<>();

        List<CreativityContextDTO.WorldRuleInfo> worldRules = new ArrayList<>();

        List<CreativityContextDTO.StoryEventInfo> events = new ArrayList<>();

        List<CreativityContextDTO.RelationInfo> relations = new ArrayList<>();


        // Read every output in a stable order
        for (ProjectOutput output : outputs.stream().sorted(Comparator.comparing(ProjectOutput::getId)).toList()) {

            // Load nodes while the caller's transaction is active
            List<ProjectNode> nodes = output.getNodes().stream().sorted(Comparator.comparing(ProjectNode::getId)).toList();

            for (ProjectNode node : nodes) {

                // CharacterProfile
                CharacterProfile character = node.getCharacterProfile();

                if (character != null) {
                    characters.add(new CreativityContextDTO.CharacterInfo(character.getId(), node.getId(), character.getName(), character.getShortDescription(), character.getRole(), character.getHistory(), character.getGoals(), character.getFears(), character.getInnerConflict(), character.getCharacterArc()));
                }
                // World
                World world = node.getWorld();

                if (world != null) {
                    worlds.add(new CreativityContextDTO.WorldInfo(world.getId(), node.getId(), world.getName(), world.getDescription(), world.getHistory(), world.getLore(), world.getAtmosphere()));
                }


                // Location
                Location location = node.getLocation();

                if (location != null) {
                    locations.add(new CreativityContextDTO.LocationInfo(location.getId(), node.getId(), location.getWorld() == null ? null : location.getWorld().getId(), location.getParentLocation() == null ? null : location.getParentLocation().getId(), location.getName(), location.getLocationType(), location.getNarrativeSignificance(), location.getNotes()));
                }


                // WorldRule
                WorldRule rule = node.getWorldRule();

                if (rule != null) {
                    worldRules.add(new CreativityContextDTO.WorldRuleInfo(rule.getId(), node.getId(), rule.getWorld() == null ? null : rule.getWorld().getId(), rule.getName(), rule.getDescription(), rule.getScope(), rule.getConditions(), rule.getExceptions(), rule.getConsequences()));
                }


                // StoryEvent
                StoryEvent event = node.getStoryEvent();

                if (event != null) {
                    events.add(new CreativityContextDTO.StoryEventInfo(event.getId(), node.getId(), event.getTitle(), event.getDescription(), event.getEventType(), event.getPresentationOrder(), event.getChronologicalOrder(), event.getInWorldTime()));
                }
            }


            // Read the relationships stored in ProjectEdges
            List<ProjectEdge> edges = output.getEdges().stream().sorted(Comparator.comparing(ProjectEdge::getId)).toList();

            for (ProjectEdge edge : edges) {

                // Game choices will have their own context later
                if ("BRANCH_CHOICE".equals(edge.getType())) {
                    continue;
                }

                ProjectNode source = edge.getSourceNode();
                ProjectNode target = edge.getTargetNode();

                relations.add(new CreativityContextDTO.RelationInfo(edge.getId(), output.getId(), edge.getType(), edge.getLabel(), source.getId(), source.getType(), target.getId(), target.getType()));
            }
        }


        // Return a structured context without modifying the project
        return new CreativityContextDTO(characters, worlds, locations, worldRules, events, relations);
    }
}
