package com.example.sana.AI.DTO;

import com.example.sana.AI.Enums.AIReplyType;

// Strict response contract for the team's future shared n8n workflow.
// Each response must be ANSWER, ASK_USER or PROPOSAL.
public record AIWorkflowResponseDTO(
        AIReplyType responseType,
        String message,
        String modelName,
        AIFollowUpDTO followUp,
        AIProposalDraftDTO proposal
) {}
