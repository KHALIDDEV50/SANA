package com.example.sana.DTO;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoryEventDTO {

    // ProjectNode fields
    @NotNull(message = "Position X cannot be null")
    private Double positionX;

    @NotNull(message = "Position Y cannot be null")
    private Double positionY;

    // StoryEvent fields
    @NotEmpty(message = "Title cannot be empty")
    private String title;

    @NotEmpty(message = "Description cannot be empty")
    private String description;

    private String eventType;

    @NotNull(message = "Presentation order cannot be null")
    private Integer presentationOrder;

    private String inWorldTime;
    private String notes;
}

