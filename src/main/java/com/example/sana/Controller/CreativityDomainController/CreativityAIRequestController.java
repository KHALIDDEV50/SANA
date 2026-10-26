package com.example.sana.Controller.CreativityDomainController;

import com.example.sana.DTO.AIDTOs.AIRunResponseDTO;
import com.example.sana.DTO.CreativityDomainDTOs.CreativityAIRequestDTO;
import com.example.sana.Service.CreativityDomainServices.CreativityAIRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Dedicated Creativity entrypoint. Other domains keep their existing /ai/requests route. */
@RestController
@RequestMapping("/api/projects/{projectId}/creativity/ai")
@RequiredArgsConstructor
public class CreativityAIRequestController {
    private final CreativityAIRequestService creativityAIRequestService;

    @PostMapping("/requests")
    public ResponseEntity<AIRunResponseDTO> start(
            @PathVariable Integer projectId,
            @Valid @RequestBody CreativityAIRequestDTO request) {
        return ResponseEntity.status(202).body(creativityAIRequestService.start(projectId, request));
    }
}
