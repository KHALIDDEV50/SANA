package com.example.sana.AI.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// The user can select a suggested answer, write another answer,
// or send "Let SANA decide" as a normal free-text reply.
public record AIReplyRequestDTO(
        @NotBlank(message = "Reply is required")
        @Size(max = 10000, message = "Reply cannot exceed 10000 characters")
        String message
) {}
