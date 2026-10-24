package com.example.sana.AI.DTO;

import com.example.sana.AI.Enums.AIMode;
import com.example.sana.AI.Enums.AITaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SanaAIRequestDTO {

    // AI usage mode
    @NotNull(message = "AI mode is required")
    private AIMode mode;

    // Specific creative task
    @NotNull(message = "AI task is required")
    private AITaskType task;

    // User's request or idea description
    @NotBlank(message = "Message cannot be empty")
    @Size(max = 10000, message = "Message is too long")
    private String message;
}
