package com.example.sana.Controller;

import com.example.sana.API.ApiResponse;
import com.example.sana.Model.Project;
import com.example.sana.Service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/project")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    // Get all projects
    @GetMapping("/get")
    public ResponseEntity<List<Project>> getAllProjects() {
        return ResponseEntity.status(200).body(projectService.getAllProjects());
    }

    // Get a project by ID
    @GetMapping("/get/{projectId}")
    public ResponseEntity<Project> getProjectById(@PathVariable Integer projectId) {

        return ResponseEntity.status(200).body(projectService.getProjectById(projectId));
    }

    // Get all projects for a specific user
    @GetMapping("/get/user/{userId}")
    public ResponseEntity<List<Project>> getProjectsByUserId(@PathVariable Integer userId) {

        return ResponseEntity.status(200).body(projectService.getProjectsByUserId(userId));
    }

    // Add a new project
    @PostMapping("/add/{userId}")
    public ResponseEntity<ApiResponse> addProject(@PathVariable Integer userId, @Valid @RequestBody Project project) {

        projectService.addProject(userId, project);

        return ResponseEntity.status(200).body(new ApiResponse("Project added successfully"));
    }

    // Update an existing project
    @PutMapping("/update/{projectId}")
    public ResponseEntity<ApiResponse> updateProject(@PathVariable Integer projectId, @Valid @RequestBody Project project) {

        projectService.updateProject(projectId, project);

        return ResponseEntity.status(200).body(new ApiResponse("Project updated successfully"));
    }

    // Delete an existing project
    @DeleteMapping("/delete/{projectId}")
    public ResponseEntity<ApiResponse> deleteProject(@PathVariable Integer projectId) {

        projectService.deleteProject(projectId);

        return ResponseEntity.status(200).body(new ApiResponse("Project deleted successfully"));
    }
}