package com.example.sana.DTO.TechDomainDTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ArchitectureEdgeRequestDTO {

    @NotBlank(message = "Architecture edge type is required")
    @Size(max = 50, message = "Architecture edge type must not exceed 50 characters")
    private String type;

    @NotBlank(message = "Architecture edge label is required")
    @Size(max = 50, message = "Architecture edge label must not exceed 50 characters")
    private String label;

    @NotNull(message = "Source node ID is required")
    @Positive(message = "Source node ID must be greater than zero")
    private Integer sourceNodeId;

    @NotNull(message = "Target node ID is required")
    @Positive(message = "Target node ID must be greater than zero")
    private Integer targetNodeId;

    private Map<String, Object> data;
}
