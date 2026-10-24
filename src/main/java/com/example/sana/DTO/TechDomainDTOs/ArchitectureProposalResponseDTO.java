package com.example.sana.DTO.TechDomainDTOs;

import com.example.sana.Enums.*;
import java.time.LocalDateTime;
import java.util.List;

public record ArchitectureProposalResponseDTO(Integer proposalId, String requestId, String title, String description,
        Integer outputId, Integer baseRevision, String reviewStatus, Integer appliedOutputId,
        GraphDefinitionDTO proposedGraph, List<Change> changes, LocalDateTime createdAt, LocalDateTime reviewedAt) {
    public record Change(ChangeOperation operation, ChangeTargetType targetType, Integer targetId, Object before, Object after) {}
}
