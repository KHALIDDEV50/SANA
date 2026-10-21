package com.example.sana.Controller;

import com.example.sana.API.ApiResponse;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Service.ProjectNodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/node")
@RequiredArgsConstructor
public class ProjectNodeController {

    private final ProjectNodeService projectNodeService;

    @GetMapping("/get")
    public ResponseEntity<List<ProjectNode>> getAllNodes() {
        return ResponseEntity.status(200).body(projectNodeService.getAllNodes());
    }

    @PostMapping("/add/{outputId}")
    public ResponseEntity<ApiResponse> addNode(@PathVariable Integer outputId, @Valid @RequestBody ProjectNode projectNode) {
        projectNodeService.addNode(outputId, projectNode);
        return ResponseEntity.status(200).body(new ApiResponse("Node added successfully"));
    }

    @PutMapping("/update/{projectNodeId}")
    public ResponseEntity<ApiResponse> updateNode(@PathVariable Integer projectNodeId, @Valid @RequestBody ProjectNode projectNode) {
        projectNodeService.updateNode(projectNodeId, projectNode);
        return ResponseEntity.status(200).body(new ApiResponse("Node updated successfully"));
    }

    @DeleteMapping("/delete/{nodeId}")
    public ResponseEntity<ApiResponse> deleteNode(@PathVariable Integer nodeId) {
        projectNodeService.deleteNode(nodeId);
        return ResponseEntity.status(200).body(new ApiResponse("Node deleted successfully"));
    }
}

