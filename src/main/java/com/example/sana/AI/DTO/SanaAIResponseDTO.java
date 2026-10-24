package com.example.sana.AI.DTO;

import com.example.sana.AI.Enums.AIMode;
import com.example.sana.AI.Enums.AITaskType;
import com.example.sana.AI.Enums.AIReplyType;

public record SanaAIResponseDTO(

        // ID of the saved AIInteraction
        Integer interactionId,

        AIMode mode,
        AITaskType task,

        // ANSWER, ASK_USER or PROPOSAL
        AIReplyType responseType,

        // The main message displayed in the chat
        String message,

        // Existing ChangeProposal ID when saved for review
        Integer proposalId,

        // Present only when the AI asks a question
        AIFollowUpDTO followUp

) {}
