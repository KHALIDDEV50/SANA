package com.example.sana.AI.DTO;

import com.example.sana.Enums.ChangeOperation;
import com.example.sana.Enums.ChangeTargetType;
import com.example.sana.Enums.ProposalStatus;

import java.util.List;

public record AIPendingProposalDTO(

        Integer proposalId,
        Integer projectId,
        Integer interactionId,

        String title,
        String description,

        ProposalStatus status,
        Integer baseVersion,

        List<ItemInfo> items

) {

    public record ItemInfo(
            Integer itemId,
            ChangeOperation operation,
            ChangeTargetType targetType,
            Integer targetId,

            // Stored snapshots and proposed changes
            String beforeJson,
            String afterJson
    ) {}
}
