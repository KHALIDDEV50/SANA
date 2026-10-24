package com.example.sana.DTO.TechDomainDTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ArchitectureGraphResponseDTO {
    private Integer outputId;
    private Integer graphRevision;
    private GraphDefinitionDTO graph;
}
