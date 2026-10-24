package com.example.sana.AI.DTO;

import com.example.sana.AI.Enums.AIReplyType;
import com.example.sana.AI.Enums.AIMode;
import com.example.sana.AI.Enums.AITaskType;

import java.time.LocalDateTime;

public record AIConversationTurnDTO(

        Integer interactionId,
        Integer parentInteractionId,

        LocalDateTime createdAt,

        AIMode mode,
        AITaskType task,

        Integer outputId,
        Integer selectedNodeId,

        String userMessage,
        String assistantMessage,

        AIReplyType responseType,
        AIFollowUpDTO followUp

) {}


//هذا خاص بعرض سجل المحادثة في الفرونت إند.
