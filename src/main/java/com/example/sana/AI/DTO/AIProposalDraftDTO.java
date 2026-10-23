package com.example.sana.AI.DTO;

import com.example.sana.Enums.ChangeOperation;
import com.example.sana.Enums.ChangeTargetType;

import java.util.List;
import java.util.Map;

public record AIProposalDraftDTO(

        String title,
        String description,

        // Proposed changes that require the user's approval
        List<ProposedItem> items

) {

    public record ProposedItem(

            // CREATE, UPDATE or DELETE
            ChangeOperation operation,

            // NODE, EDGE or RECORD
            ChangeTargetType targetType,

            // Required for UPDATE and DELETE
            Integer targetId,

            // Output used for creating canvas elements
            Integer outputId,

            // Example: CHARACTER, WORLD or STORY_EVENT
            String elementType,

            // Only the suggested fields, not a JPA entity
            Map<String, Object> proposedFields

    ) {}
}
