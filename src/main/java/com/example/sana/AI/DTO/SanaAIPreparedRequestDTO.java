package com.example.sana.AI.DTO;

import com.example.sana.AI.Enums.AIMode;
import com.example.sana.AI.Enums.AITaskType;
import com.example.sana.AI.Enums.AIContextScope;

public record SanaAIPreparedRequestDTO(

        // Project ID received from the API path
        Integer projectId,

        // AI request details
        AIMode mode,
        AITaskType task,
        String message,

        // Optional IDs received from the API path
        Integer outputId,
        Integer selectedNodeId,

        // Context selected by the backend
        AIContextScope contextScope,

        // Relevant project information
        ProjectContextDTO context

) {}
