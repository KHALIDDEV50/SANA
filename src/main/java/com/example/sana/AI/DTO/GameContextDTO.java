package com.example.sana.AI.DTO;

import java.util.List;

public record GameContextDTO(

        List<SceneInfo> scenes,
        List<ChoiceInfo> choices,
        List<VariableInfo> variables,
        List<PlayableCharacterInfo> playableCharacters

) {

    // A scene inside a branching narrative
    public record SceneInfo(
            Integer id,
            Integer projectNodeId,
            Integer outputId,
            String title,
            String content,
            String type,
            Integer speakerCharacterId,
            Integer relatedStoryEventId
    ) {}


    // A player choice connecting two scenes
    public record ChoiceInfo(
            Integer id,
            // Target ID to use for ChangeTargetType.EDGE updates
            Integer projectEdgeId,
            Integer outputId,
            Integer sourceSceneId,
            Integer targetSceneId,
            String text,
            Integer displayOrder,

            // Optional condition
            String conditionVariable,
            String conditionOperator,
            Integer conditionValue,

            // Optional effect
            String effectVariable,
            String effectOperator,
            Integer effectValue
    ) {}


    // A variable used during game preview
    public record VariableInfo(
            Integer id,
            Integer outputId,
            String name,
            Integer initialValue
    ) {}


    // A selectable character with optional gameplay stats
    public record PlayableCharacterInfo(
            Integer id,
            Integer outputId,
            Integer characterProfileId,
            String name,
            String role,
            String weaponName,
            String weaponType,
            String strengths,
            String weaknesses,

            Integer attack,
            Integer defense,
            Integer speed,
            Integer rangeStat,
            Integer stamina,
            Integer special
    ) {}
}
