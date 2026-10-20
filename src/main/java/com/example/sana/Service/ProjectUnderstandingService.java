package com.example.sana.Service;

import com.example.sana.API.ApiException;
import com.example.sana.Model.Project;
import com.example.sana.Model.ProjectUnderstanding;
import com.example.sana.Repository.ProjectRepository;
import com.example.sana.Repository.ProjectUnderstandingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectUnderstandingService {

    private final ProjectUnderstandingRepository projectUnderstandingRepository;
    private final ProjectRepository projectRepository;

    // Get all project understandings
    public List<ProjectUnderstanding> getAllProjectUnderstandings() {
        return projectUnderstandingRepository.findAll();
    }

    // Get project understanding by ID
    public ProjectUnderstanding getProjectUnderstandingById(Long id) {

        ProjectUnderstanding understanding = projectUnderstandingRepository.findProjectUnderstandingById(id);

        if (understanding == null) {
            throw new ApiException("Project understanding not found");
        }

        return understanding;
    }

    // Get project understanding by project ID
    public ProjectUnderstanding getProjectUnderstandingByProjectId(Long projectId) {

        ProjectUnderstanding understanding = projectUnderstandingRepository.findProjectUnderstandingByProject_Id(projectId);

        if (understanding == null) {
            throw new ApiException("Project understanding not found");
        }

        return understanding;
    }

    // Add project understanding to an existing project
    public void addProjectUnderstanding(Long projectId, ProjectUnderstanding projectUnderstanding) {

        // Check if project exists
        Project project = projectRepository.findProjectById(projectId);

        if (project == null) {
            throw new ApiException("Project not found");
        }

        // Prevent multiple understandings for the same project
        ProjectUnderstanding existingUnderstanding = projectUnderstandingRepository.findProjectUnderstandingByProject_Id(projectId);

        if (existingUnderstanding != null) {
            throw new ApiException("Project understanding already exists for this project");
        }


        projectUnderstanding.setProject(project);
        projectUnderstanding.setCreatedAt(LocalDateTime.now());
        projectUnderstanding.setUpdatedAt(LocalDateTime.now());

        projectUnderstandingRepository.save(projectUnderstanding);
    }

    // Update an existing project understanding
    public void updateProjectUnderstanding(Long id, ProjectUnderstanding projectUnderstanding) {

        // Check if project understanding exists
        ProjectUnderstanding oldUnderstanding = projectUnderstandingRepository.findProjectUnderstandingById(id);

        if (oldUnderstanding == null) {
            throw new ApiException("Project understanding not found");
        }


        oldUnderstanding.setProjectGoal(projectUnderstanding.getProjectGoal());
        oldUnderstanding.setActorsJson(projectUnderstanding.getActorsJson());
        oldUnderstanding.setComponentsJson(projectUnderstanding.getComponentsJson());
        oldUnderstanding.setConstraintsJson(projectUnderstanding.getConstraintsJson());
        oldUnderstanding.setUpdatedAt(LocalDateTime.now());

        projectUnderstandingRepository.save(oldUnderstanding);
    }

    // Delete an existing project understanding
    public void deleteProjectUnderstanding(Long id) {

        ProjectUnderstanding understanding = projectUnderstandingRepository.findProjectUnderstandingById(id);

        if (understanding == null) {
            throw new ApiException("Project understanding not found");
        }

        projectUnderstandingRepository.delete(understanding);
    }
}