package com.example.sana.AI.DTO;

import com.example.sana.AI.Enums.AIMode;
import com.example.sana.AI.Enums.AITaskType;
import com.example.sana.AI.Enums.AIReplyType;

public record AIConversationPayloadDTO(

        // Previous interaction in this conversation
        Integer parentInteractionId,

        // Original AI request information
        AIMode mode,
        AITaskType task,

        // Optional context selection
        Integer outputId,
        Integer selectedNodeId,

        // AI response
        AIReplyType responseType,
        String assistantMessage,

        // Question details when responseType is ASK_USER
        AIFollowUpDTO followUp

) {}

//هذا الـ DTO هو شكل البيانات اللي بنخزنها داخل responseJson