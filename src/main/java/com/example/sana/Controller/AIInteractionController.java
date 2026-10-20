package com.example.sana.Controller;

import com.example.sana.API.ApiResponse;
import com.example.sana.Model.AIInteraction;
import com.example.sana.Service.AIInteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai-interactions")
@RequiredArgsConstructor
public class AIInteractionController {

    private final AIInteractionService aiInteractionService;

    @GetMapping
    public ResponseEntity<?> getAllAIInteractions() {
        return ResponseEntity.status(200).body(aiInteractionService.getAllAIInteractions());
    }

    @GetMapping("/{ai_interaction_id}")
    public ResponseEntity<?> getAIInteractionById(@PathVariable Integer ai_interaction_id) {
        return ResponseEntity.status(200).body(aiInteractionService.getAIInteractionById(ai_interaction_id));
    }

    @PostMapping
    public ResponseEntity<?> addAIInteraction(@RequestParam Long user_id, @RequestParam Long project_id,
                                             @Valid @RequestBody AIInteraction aiInteraction) {
        aiInteractionService.addAIInteraction(user_id, project_id, aiInteraction);
        return ResponseEntity.status(200).body(new ApiResponse("AI interaction added successfully"));
    }

    @PutMapping("/{ai_interaction_id}")
    public ResponseEntity<?> updateAIInteraction(@PathVariable Integer ai_interaction_id,
                                                @Valid @RequestBody AIInteraction aiInteraction) {
        aiInteractionService.updateAIInteraction(ai_interaction_id, aiInteraction);
        return ResponseEntity.status(200).body(new ApiResponse("AI interaction updated successfully"));
    }

    @DeleteMapping("/{ai_interaction_id}")
    public ResponseEntity<?> deleteAIInteraction(@PathVariable Integer ai_interaction_id) {
        aiInteractionService.deleteAIInteraction(ai_interaction_id);
        return ResponseEntity.status(200).body(new ApiResponse("AI interaction deleted successfully"));
    }
}
