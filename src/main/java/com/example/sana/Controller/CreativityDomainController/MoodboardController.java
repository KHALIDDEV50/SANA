package com.example.sana.Controller.CreativityDomainController;

import com.example.sana.API.ApiResponse;
import com.example.sana.DTO.CreativityDomainDTOs.MoodboardDTO;
import com.example.sana.Service.CreativityDomainServices.MoodboardService;
import com.example.sana.Service.CreativityDomainServices.OpenverseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/creativity/moodboard")
@RequiredArgsConstructor
public class MoodboardController {

    private final MoodboardService moodboardService;
    private final OpenverseService openverseService;

    // Search Openverse without a key; images are not saved by searching.
    @GetMapping("/images/search")
    public ResponseEntity<?> searchImages(@RequestParam String keyword,
                                          @RequestParam(required = false) Integer page,
                                          @RequestParam(required = false) Integer pageSize) {
        return ResponseEntity.status(200).body(openverseService.searchImages(keyword, page, pageSize));
    }

    // Fetch complete image attribution before selecting an image.
    @GetMapping("/images/{openverseId}")
    public ResponseEntity<?> imageDetails(@PathVariable String openverseId) {
        return ResponseEntity.status(200).body(openverseService.getImage(openverseId));
    }

    // Create one board inside an existing SANA Project.
    @PostMapping("/add/{projectId}")
    public ResponseEntity<?> createBoard(@PathVariable Integer projectId, @Valid @RequestBody MoodboardDTO.CreateBoard request) {
        return ResponseEntity.status(200).body(moodboardService.createBoard(projectId, request));
    }

    @GetMapping("/get/project/{projectId}")
    public ResponseEntity<?> projectBoards(@PathVariable Integer projectId) {
        return ResponseEntity.status(200).body(moodboardService.getProjectBoards(projectId));
    }

    @GetMapping("/get/{outputId}")
    public ResponseEntity<?> getBoard(@PathVariable Integer outputId) {
        return ResponseEntity.status(200).body(moodboardService.getBoard(outputId));
    }

    @PutMapping("/update/{outputId}/title")
    public ResponseEntity<?> renameBoard(@PathVariable Integer outputId, @Valid @RequestBody MoodboardDTO.RenameBoard request) {
        return ResponseEntity.status(200).body(moodboardService.renameBoard(outputId, request));
    }

    // Add a verified Openverse image as a standalone ProjectNode.
    @PostMapping("/add/{outputId}/images")
    public ResponseEntity<?> addImage(@PathVariable Integer outputId, @Valid @RequestBody MoodboardDTO.AddImage request) {
        return ResponseEntity.status(200).body(moodboardService.addImage(outputId, request));
    }

    // Add an independent, freely movable text label.
    @PostMapping("/add/{outputId}/labels")
    public ResponseEntity<?> addLabel(@PathVariable Integer outputId, @Valid @RequestBody MoodboardDTO.AddLabel request) {
        return ResponseEntity.status(200).body(moodboardService.addLabel(outputId, request));
    }

    // Save X/Y after drag-and-drop for either image or label.
    @PutMapping("/update/{outputId}/items/{itemId}/position")
    public ResponseEntity<?> moveItem(@PathVariable Integer outputId, @PathVariable Integer itemId,
                                      @Valid @RequestBody MoodboardDTO.MoveItem request) {
        return ResponseEntity.status(200).body(moodboardService.moveItem(outputId, itemId, request));
    }

    @PutMapping("/update/{outputId}/labels/{itemId}")
    public ResponseEntity<?> editLabel(@PathVariable Integer outputId, @PathVariable Integer itemId,
                                       @Valid @RequestBody MoodboardDTO.EditLabel request) {
        return ResponseEntity.status(200).body(moodboardService.editLabel(outputId, itemId, request));
    }

    @DeleteMapping("/delete/{outputId}/items/{itemId}")
    public ResponseEntity<?> deleteItem(@PathVariable Integer outputId, @PathVariable Integer itemId) {
        moodboardService.deleteItem(outputId, itemId);
        return ResponseEntity.status(200).body(new ApiResponse("Moodboard item deleted successfully"));
    }
}
