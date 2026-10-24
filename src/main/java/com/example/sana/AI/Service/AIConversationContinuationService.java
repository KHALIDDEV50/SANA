package com.example.sana.AI.Service;

import com.example.sana.AI.DTO.*;
import com.example.sana.AI.Enums.AIReplyType;
import com.example.sana.API.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AIConversationContinuationService {

    private final AIConversationService conversationService;
    private final SanaAIRequestService requestService;

    // Prepare the next AI request while preserving the original mode/task/selection.
    // Only an ASK_USER interaction can receive a user follow-up response.
    @Transactional(readOnly = true)
    public SanaAIPreparedRequestDTO prepareReply(Integer projectId, Integer authenticatedUserId, Integer interactionId, AIReplyRequestDTO reply) {
        if (reply == null || reply.message() == null || reply.message().isBlank() || reply.message().length() > 10000) throw new ApiException("Invalid AI reply");
        List<AIConversationTurnDTO> history = conversationService.getConversationHistory(projectId, authenticatedUserId, interactionId);
        if (history.isEmpty()) throw new ApiException("AI conversation not found");
        AIConversationTurnDTO last = history.get(history.size() - 1);
        if (last.responseType() != AIReplyType.ASK_USER) throw new ApiException("The previous AI turn is not awaiting a reply");
        SanaAIRequestDTO request = new SanaAIRequestDTO();
        request.setMode(last.mode());
        request.setTask(last.task());
        request.setMessage(reply.message().trim());
        return requestService.prepareRequest(projectId, last.outputId(), last.selectedNodeId(), request);
    }
}
