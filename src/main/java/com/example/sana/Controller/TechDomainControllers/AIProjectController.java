package com.example.sana.Controller.TechDomainControllers;

import com.example.sana.DTO.TechDomainDTOs.*;
import com.example.sana.Service.TechDomainServices.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/ai")
@RequiredArgsConstructor
public class AIProjectController {
    private final ArchitectureAIService requests;
    private final ArchitectureProposalService proposals;

    @PostMapping("/requests")
    public ResponseEntity<ArchitectureAIRunResponseDTO> start(@PathVariable Integer projectId, @Valid @RequestBody ArchitectureAIRequestDTO request) {
        return ResponseEntity.accepted().body(requests.start(projectId, request));
    }
    @GetMapping("/requests/{requestId}")
    public ArchitectureAIRunResponseDTO status(@PathVariable Integer projectId, @PathVariable UUID requestId) {
        return requests.getRun(projectId, requestId);
    }
    @GetMapping("/proposals/{proposalId}")
    public ArchitectureProposalResponseDTO preview(@PathVariable Integer projectId, @PathVariable Integer proposalId) {
        return proposals.preview(projectId, proposalId);
    }
    @PostMapping("/proposals/{proposalId}/accept")
    public ArchitectureAcceptanceDTO accept(@PathVariable Integer projectId, @PathVariable Integer proposalId) {
        return proposals.accept(projectId, proposalId);
    }
    @PostMapping("/proposals/{proposalId}/reject")
    public ArchitectureProposalResponseDTO reject(@PathVariable Integer projectId, @PathVariable Integer proposalId) {
        return proposals.reject(projectId, proposalId);
    }
}
