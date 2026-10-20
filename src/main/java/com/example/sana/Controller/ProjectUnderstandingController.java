package com.example.sana.Controller;

import com.example.sana.API.ApiResponse;
import com.example.sana.Model.ProjectUnderstanding;
import com.example.sana.Service.ProjectUnderstandingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/project-understanding")
@RequiredArgsConstructor
public class ProjectUnderstandingController {

    private final ProjectUnderstandingService projectUnderstandingService;

    // Get all project understandings
    @GetMapping("/get")
    public ResponseEntity<List<ProjectUnderstanding>> getAllProjectUnderstandings() {
        return ResponseEntity.status(200).body(projectUnderstandingService.getAllProjectUnderstandings());
    }

    // Get project understanding by ID
    @GetMapping("/get/{id}")
    public ResponseEntity<ProjectUnderstanding> getProjectUnderstandingById(@PathVariable Long id) {

        return ResponseEntity.status(200).body(projectUnderstandingService.getProjectUnderstandingById(id));
    }

    // Get project understanding by project ID
    @GetMapping("/get/project/{projectId}")
    public ResponseEntity<ProjectUnderstanding> getProjectUnderstandingByProjectId(@PathVariable Long projectId) {

        return ResponseEntity.status(200).body(projectUnderstandingService.getProjectUnderstandingByProjectId(projectId));
    }

    // Add project understanding
    @PostMapping("/add/{projectId}")
    public ResponseEntity<ApiResponse> addProjectUnderstanding(@PathVariable Long projectId, @Valid @RequestBody ProjectUnderstanding projectUnderstanding) {

        projectUnderstandingService.addProjectUnderstanding(projectId, projectUnderstanding);

        return ResponseEntity.status(200).body(new ApiResponse("Project understanding added successfully"));
    }

    // Update project understanding
    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse> updateProjectUnderstanding(@PathVariable Long id, @Valid @RequestBody ProjectUnderstanding projectUnderstanding) {

        projectUnderstandingService.updateProjectUnderstanding(id, projectUnderstanding);

        return ResponseEntity.status(200).body(new ApiResponse("Project understanding updated successfully"));
    }

    // Delete project understanding
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse> deleteProjectUnderstanding(@PathVariable Long id) {

        projectUnderstandingService.deleteProjectUnderstanding(id);

        return ResponseEntity.status(200).body(new ApiResponse("Project understanding deleted successfully"));
    }
}