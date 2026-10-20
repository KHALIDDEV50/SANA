package com.example.sana.Controller;

import com.example.sana.API.ApiResponse;
import com.example.sana.Model.ChangeProposal;
import com.example.sana.Service.ChangeProposalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/change-proposals")
@RequiredArgsConstructor
public class ChangeProposalController {

    private final ChangeProposalService changeProposalService;

    @GetMapping
    public ResponseEntity<?> getAllChangeProposals() {
        return ResponseEntity.status(200).body(changeProposalService.getAllChangeProposals());
    }

    @GetMapping("/{proposal_id}")
    public ResponseEntity<?> getChangeProposalById(@PathVariable Integer proposal_id) {
        return ResponseEntity.status(200).body(changeProposalService.getChangeProposalById(proposal_id));
    }

    @PostMapping
    public ResponseEntity<?> addChangeProposal(@RequestParam Long project_id,
                                               @RequestParam(required = false) Integer ai_interaction_id,
                                               @Valid @RequestBody ChangeProposal changeProposal) {
        changeProposalService.addChangeProposal(project_id, ai_interaction_id, changeProposal);
        return ResponseEntity.status(200).body(new ApiResponse("Change proposal added successfully"));
    }

    @PutMapping("/{proposal_id}")
    public ResponseEntity<?> updateChangeProposal(@PathVariable Integer proposal_id,
                                                  @Valid @RequestBody ChangeProposal changeProposal) {
        changeProposalService.updateChangeProposal(proposal_id, changeProposal);
        return ResponseEntity.status(200).body(new ApiResponse("Change proposal updated successfully"));
    }

    @DeleteMapping("/{proposal_id}")
    public ResponseEntity<?> deleteChangeProposal(@PathVariable Integer proposal_id) {
        changeProposalService.deleteChangeProposal(proposal_id);
        return ResponseEntity.status(200).body(new ApiResponse("Change proposal deleted successfully"));
    }
}
