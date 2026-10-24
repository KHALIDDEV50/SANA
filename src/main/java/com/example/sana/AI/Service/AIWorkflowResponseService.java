package com.example.sana.AI.Service;

import com.example.sana.AI.DTO.*;
import com.example.sana.AI.Enums.AIReplyType;
import com.example.sana.API.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AIWorkflowResponseService {

    private final AIConversationService conversationService;
    private final AIPendingProposalService pendingProposalService;

    // Process an already-validated response from the shared n8n workflow.
    // No network calls are performed here; transport/authentication come later.
    // Only the PROPOSAL branch saves pending changes, never applies them.
    public SanaAIResponseDTO handle(Integer projectId, Integer authenticatedUserId, Integer parentInteractionId,
                                    SanaAIPreparedRequestDTO prepared, AIWorkflowResponseDTO response) {
        if (response == null || response.responseType() == null) throw new ApiException("AI response type is required");
        if (response.message() == null || response.message().isBlank()) throw new ApiException("AI message cannot be empty");
        if (response.modelName() == null || response.modelName().isBlank()) throw new ApiException("AI model name is required");
        switch (response.responseType()) {
            case ANSWER:
                if (response.followUp() != null || response.proposal() != null) throw new ApiException("ANSWER cannot contain a proposal or follow-up");
                return conversationService.saveCompletedTurn(projectId, authenticatedUserId, parentInteractionId, prepared, AIReplyType.ANSWER, response.message(), null, response.modelName());
            case ASK_USER:
                if (response.followUp() == null || response.proposal() != null) throw new ApiException("ASK_USER requires a question without a proposal");
                return conversationService.saveCompletedTurn(projectId, authenticatedUserId, parentInteractionId, prepared, AIReplyType.ASK_USER, response.message(), response.followUp(), response.modelName());
            case PROPOSAL:
                if (response.proposal() == null || response.followUp() != null) throw new ApiException("PROPOSAL requires a draft without follow-up");
                return pendingProposalService.savePendingProposal(projectId, authenticatedUserId, parentInteractionId, prepared, response.message(), response.modelName(), response.proposal());
            default:
                throw new ApiException("Unsupported AI response type");
        }
    }
}
