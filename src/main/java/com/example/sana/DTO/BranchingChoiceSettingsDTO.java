package com.example.sana.DTO;

import com.example.sana.Enums.BranchingComparisonOperator;
import com.example.sana.Enums.BranchingEffectOperator;

public record BranchingChoiceSettingsDTO(

        Integer choiceId,
        Integer sourceNodeId,
        Integer targetNodeId,

        String text,
        Integer displayOrder,

        // Optional condition
        Integer conditionVariableId,
        String conditionVariableName,
        BranchingComparisonOperator conditionOperator,
        Integer conditionValue,

        // Optional effect
        Integer effectVariableId,
        String effectVariableName,
        BranchingEffectOperator effectOperator,
        Integer effectValue
) {}
