package com.example.sana.Controller;

import com.example.sana.API.ApiResponse;
import com.example.sana.Model.ChangeProposalItem;
import com.example.sana.Service.ChangeProposalItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/change-proposal-items")
@RequiredArgsConstructor
public class ChangeProposalItemController {

    private final ChangeProposalItemService changeProposalItemService;

    @GetMapping
    public ResponseEntity<?> getAllChangeProposalItems() {
        return ResponseEntity.status(200).body(changeProposalItemService.getAllChangeProposalItems());
    }

    @GetMapping("/{item_id}")
    public ResponseEntity<?> getChangeProposalItemById(@PathVariable Integer item_id) {
        return ResponseEntity.status(200).body(changeProposalItemService.getChangeProposalItemById(item_id));
    }

    @PostMapping
    public ResponseEntity<?> addChangeProposalItem(@RequestParam Integer proposal_id,
                                                   @Valid @RequestBody ChangeProposalItem changeProposalItem) {
        changeProposalItemService.addChangeProposalItem(proposal_id, changeProposalItem);
        return ResponseEntity.status(200).body(new ApiResponse("Change proposal item added successfully"));
    }

    @PutMapping("/{item_id}")
    public ResponseEntity<?> updateChangeProposalItem(@PathVariable Integer item_id,
                                                      @Valid @RequestBody ChangeProposalItem changeProposalItem) {
        changeProposalItemService.updateChangeProposalItem(item_id, changeProposalItem);
        return ResponseEntity.status(200).body(new ApiResponse("Change proposal item updated successfully"));
    }

    @DeleteMapping("/{item_id}")
    public ResponseEntity<?> deleteChangeProposalItem(@PathVariable Integer item_id) {
        changeProposalItemService.deleteChangeProposalItem(item_id);
        return ResponseEntity.status(200).body(new ApiResponse("Change proposal item deleted successfully"));
    }
}
