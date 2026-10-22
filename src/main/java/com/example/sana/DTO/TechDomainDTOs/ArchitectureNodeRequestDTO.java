package com.example.sana.DTO.TechDomainDTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ArchitectureNodeRequestDTO {

    @NotBlank(message = "Architecture node type is required")
    @Size(max = 50, message = "Architecture node type must not exceed 50 characters")
    private String type;

    @NotBlank(message = "Architecture node label is required")
    @Size(max = 255, message = "Architecture node label must not exceed 255 characters")
    private String label;

    @NotNull(message = "Architecture node position X is required")
    private Double positionX;

    @NotNull(message = "Architecture node position Y is required")
    private Double positionY;

    private Map<String, Object> data;
}
