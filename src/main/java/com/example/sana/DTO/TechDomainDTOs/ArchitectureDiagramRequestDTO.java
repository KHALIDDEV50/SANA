package com.example.sana.DTO.TechDomainDTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArchitectureDiagramRequestDTO {

    @NotBlank(message = "Architecture diagram title is required")
    @Size(max = 255, message = "Architecture diagram title must not exceed 255 characters")
    private String title;
}
