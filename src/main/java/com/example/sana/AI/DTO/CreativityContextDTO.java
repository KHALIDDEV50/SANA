package com.example.sana.AI.DTO;

import java.util.List;

public record CreativityContextDTO(

        List<CharacterInfo> characters,
        List<WorldInfo> worlds,
        List<LocationInfo> locations,
        List<WorldRuleInfo> worldRules,
        List<StoryEventInfo> storyEvents,
        List<RelationInfo> relations

) {

    // Story characters and their development
    public record CharacterInfo(
            Integer id,
            Integer nodeId,
            String name,
            String description,
            String role,
            String history,
            String goals,
            String fears,
            String innerConflict,
            String characterArc
    ) {}


    // Fictional worlds and their background
    public record WorldInfo(
            Integer id,
            Integer nodeId,
            String name,
            String description,
            String history,
            String lore,
            String atmosphere
    ) {}


    // Locations inside the fictional world
    public record LocationInfo(
            Integer id,
            Integer nodeId,
            Integer worldId,
            Integer parentLocationId,
            String name,
            String locationType,
            String narrativeSignificance,
            String notes
    ) {}


    // Rules that control the fictional world
    public record WorldRuleInfo(
            Integer id,
            Integer nodeId,
            Integer worldId,
            String name,
            String description,
            String scope,
            String conditions,
            String exceptions,
            String consequences
    ) {}


    // Story events and their timeline positions
    public record StoryEventInfo(
            Integer id,
            Integer nodeId,
            String title,
            String description,
            String eventType,
            Integer presentationOrder,
            Integer chronologicalOrder,
            String inWorldTime
    ) {}


    // Connections between project elements
    public record RelationInfo(
            Integer edgeId,
            Integer outputId,
            String type,
            String label,
            Integer sourceNodeId,
            String sourceNodeType,
            Integer targetNodeId,
            String targetNodeType
    ) {}
}
