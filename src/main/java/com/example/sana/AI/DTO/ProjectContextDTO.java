package com.example.sana.AI.DTO;

import java.util.List;

public record ProjectContextDTO(

        Integer projectId,
        String projectName,
        String description,
        String domain,
        String projectStatus,
        String projectGoal,

        List<OutputInfo> outputs,

        // Story and world-building context
        CreativityContextDTO creativity,

        // Branching games and playable character context
        GameContextDTO game

) {

    public record OutputInfo(
            Integer outputId,
            String title,
            String outputType,
            String representationType
    ) {}
}
