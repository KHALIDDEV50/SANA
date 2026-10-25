package com.example.sana.Controller;

import com.example.sana.DTO.AIDTOs.AIProposalAcceptanceDTO;
import com.example.sana.DTO.AIDTOs.AIProposalResponseDTO;
import com.example.sana.DTO.AIDTOs.AIRequestDTO;
import com.example.sana.DTO.AIDTOs.AIRunResponseDTO;
import com.example.sana.Service.AIServices.AIProposalService;
import com.example.sana.Service.AIServices.AIRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// AI requests and proposal review for every domain. The domain comes from the stored project.
@RestController
@RequestMapping("/api/projects/{projectId}/ai")
@RequiredArgsConstructor
public class AIRequestController {

    private final AIRequestService aiRequestService;
    private final AIProposalService aiProposalService;

    // Start a CREATE, EDIT or ADVISE request; the result arrives later through the n8n callback
    @PostMapping("/requests")
    public ResponseEntity<AIRunResponseDTO> startRequest(@PathVariable Integer projectId, @Valid @RequestBody AIRequestDTO request) {
        return ResponseEntity.status(202).body(aiRequestService.start(projectId, request));
    }

    // Poll the state and result of a request
    @GetMapping("/requests/{requestId}")
    public ResponseEntity<AIRunResponseDTO> getRequest(@PathVariable Integer projectId, @PathVariable UUID requestId) {
        return ResponseEntity.status(200).body(aiRequestService.getRun(projectId, requestId));
    }

    // Show a proposed diagram and its computed changes
    @GetMapping("/proposals/{proposalId}")
    public ResponseEntity<AIProposalResponseDTO> previewProposal(@PathVariable Integer projectId, @PathVariable Integer proposalId) {
        return ResponseEntity.status(200).body(aiProposalService.preview(projectId, proposalId));
    }

    // Apply a pending proposal to the diagram
    @PostMapping("/proposals/{proposalId}/accept")
    public ResponseEntity<AIProposalAcceptanceDTO> acceptProposal(@PathVariable Integer projectId, @PathVariable Integer proposalId) {
        return ResponseEntity.status(200).body(aiProposalService.accept(projectId, proposalId));
    }

    // Reject a pending proposal without changing the diagram
    @PostMapping("/proposals/{proposalId}/reject")
    public ResponseEntity<AIProposalResponseDTO> rejectProposal(@PathVariable Integer projectId, @PathVariable Integer proposalId) {
        return ResponseEntity.status(200).body(aiProposalService.reject(projectId, proposalId));
    }
}
