package com.example.sana.Controller;

import com.example.sana.API.ApiResponse;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Service.ProjectOutputService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/project-output")
@RequiredArgsConstructor
public class ProjectOutputController {

    private final ProjectOutputService projectOutputService;

    @GetMapping("/get")
    public ResponseEntity<List<ProjectOutput>> getAllProjectOutputs() {
        return ResponseEntity.status(200).body(projectOutputService.getAllProjectOutputs());
    }

    @GetMapping("/get/{outputId}")
    public ResponseEntity<ProjectOutput> getProjectOutputById(@PathVariable Integer outputId) {
        return ResponseEntity.status(200).body(projectOutputService.getProjectOutputById(outputId));
    }

    @GetMapping("/get/project/{projectId}")
    public ResponseEntity<List<ProjectOutput>> getProjectOutputsByProjectId(@PathVariable Integer projectId) {
        return ResponseEntity.status(200).body(projectOutputService.getProjectOutputsByProjectId(projectId));
    }

    @PostMapping("/add/{projectId}")
    public ResponseEntity<ApiResponse> addProjectOutput(@PathVariable Integer projectId,
                                                      @Valid @RequestBody ProjectOutput projectOutput) {
        projectOutputService.addProjectOutput(projectId, projectOutput);
        return ResponseEntity.status(200).body(new ApiResponse("Project output added successfully"));
    }

    @PutMapping("/update/{outputId}")
    public ResponseEntity<ApiResponse> updateProjectOutput(@PathVariable Integer outputId,
                                                         @Valid @RequestBody ProjectOutput projectOutput) {
        projectOutputService.updateProjectOutput(outputId, projectOutput);
        return ResponseEntity.status(200).body(new ApiResponse("Project output updated successfully"));
    }

    @DeleteMapping("/delete/{outputId}")
    public ResponseEntity<ApiResponse> deleteProjectOutput(@PathVariable Integer outputId) {
        projectOutputService.deleteProjectOutput(outputId);
        return ResponseEntity.status(200).body(new ApiResponse("Project output deleted successfully"));
    }
}
