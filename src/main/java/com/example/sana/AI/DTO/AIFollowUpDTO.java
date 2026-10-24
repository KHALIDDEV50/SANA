package com.example.sana.AI.DTO;

import java.util.List;

public record AIFollowUpDTO(

        // One clear question for the user
        String question,

        // Optional choices that make answering easier
        List<String> suggestedAnswers,

        // Explain briefly why the answer matters
        String reason,

        // Whether the user can skip this question
        boolean canSkip

) {}
