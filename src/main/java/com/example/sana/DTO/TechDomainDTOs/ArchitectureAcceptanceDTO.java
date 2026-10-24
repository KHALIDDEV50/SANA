package com.example.sana.DTO.TechDomainDTOs;

import java.util.Map;
public record ArchitectureAcceptanceDTO(Integer proposalId, String status, Integer outputId, Integer graphRevision, Map<String, Integer> elementIds) {}
