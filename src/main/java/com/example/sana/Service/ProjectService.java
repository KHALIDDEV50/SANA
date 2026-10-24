package com.example.sana.Service;

import com.example.sana.API.ApiException;
import com.example.sana.Model.Project;
import com.example.sana.Model.User;
import com.example.sana.Repository.ProjectRepository;
import com.example.sana.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    // Get all projects
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    // Get a project by ID
    public Project getProjectById(Integer projectId) {

        Project project = projectRepository.findProjectById(projectId);

        if (project == null) {
            throw new ApiException("Project not found");
        }

        return project;
    }

    // Get all projects owned by a specific user
    public List<Project> getProjectsByUserId(Integer userId) {

        User user = userRepository.findUserById(userId);

        if (user == null) {
            throw new ApiException("User not found");
        }

        return projectRepository.findAllByOwner_Id(userId);
    }

    // Add a new project for a specific user
    public void addProject(Integer userId, Project project) {

        // Check if the owner exists
        User owner = userRepository.findUserById(userId);

        if (owner == null) {
            throw new ApiException("User not found");
        }

        project.setId(null); project.setVersion(0); project.setOutputs(new HashSet<>()); project.setRecords(new HashSet<>()); project.setUnderstanding(null);
        project.setOwner(owner);
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());

        projectRepository.save(project);
    }

    // Update an existing project
    @Transactional
    public void updateProject(Integer projectId, Project project) {

        // Check if the project exists
        Project oldProject = projectRepository.findByIdForUpdate(projectId).orElse(null);

        if (oldProject == null) {
            throw new ApiException("Project not found");
        }


        oldProject.setName(project.getName());
        oldProject.setDescription(project.getDescription());
        oldProject.setDomain(project.getDomain());
        oldProject.setStatus(project.getStatus());
        oldProject.setUpdatedAt(LocalDateTime.now());

        projectRepository.save(oldProject);
    }

    // Delete an existing project
    public void deleteProject(Integer projectId) {

        // Check if the project exists
        Project project = projectRepository.findProjectById(projectId);

        if (project == null) {
            throw new ApiException("Project not found");
        }

        projectRepository.delete(project);
    }
}