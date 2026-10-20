package com.example.sana.Controller;

import com.example.sana.API.ApiResponse;
import com.example.sana.Model.ProjectEdge;
import com.example.sana.Service.ProjectEdgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/edge")
@RequiredArgsConstructor
public class ProjectEdgeController {

    private final ProjectEdgeService projectEdgeService;

    @GetMapping("/get")
    public ResponseEntity<List<ProjectEdge>> getAllEdges() {
        return ResponseEntity.status(200).body(projectEdgeService.getAllEdges());
    }

    @PostMapping("/add/{projectId}")
    public ResponseEntity<ApiResponse> addEdge(@PathVariable Integer projectId, @Valid @RequestBody ProjectEdge projectEdge) {
        projectEdgeService.addEdge(projectId, projectEdge);
        return ResponseEntity.status(200).body(new ApiResponse("Edge added successfully"));
    }

    @PutMapping("/update/{projectEdgeId}")
    public ResponseEntity<ApiResponse> updateEdge(@PathVariable Integer projectEdgeId, @Valid @RequestBody ProjectEdge projectEdge) {
        projectEdgeService.updateEdge(projectEdgeId, projectEdge);
        return ResponseEntity.status(200).body(new ApiResponse("Edge updated successfully"));
    }

    @DeleteMapping("/delete/{edgeId}")
    public ResponseEntity<ApiResponse> deleteEdge(@PathVariable Integer edgeId) {
        projectEdgeService.deleteEdge(edgeId);
        return ResponseEntity.status(200).body(new ApiResponse("Edge deleted successfully"));
    }
}
