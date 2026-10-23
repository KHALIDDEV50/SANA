package com.example.sana.AI.Service;

import com.example.sana.AI.DTO.CreativityContextDTO;
import com.example.sana.AI.DTO.GameContextDTO;
import com.example.sana.AI.DTO.ProjectContextDTO;
import com.example.sana.API.ApiException;
import com.example.sana.Model.Project;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Model.ProjectUnderstanding;
import com.example.sana.Repository.ProjectRepository;
import com.example.sana.Repository.ProjectOutputRepository;
import com.example.sana.Repository.ProjectUnderstandingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectContextService {

    private final ProjectRepository projectRepository;
    private final ProjectOutputRepository projectOutputRepository;
    private final ProjectUnderstandingRepository projectUnderstandingRepository;
    private final CreativityContextService creativityContextService;
    private final GameContextService gameContextService;


    // Build the basic project context for AI requests
    @Transactional(readOnly = true)
    public ProjectContextDTO buildProjectContext(Integer projectId) {

        // Find the requested project
        Project project = projectRepository.findProjectById(projectId);

        if (project == null) {
            throw new ApiException("Project not found");
        }

        // Get the project's existing understanding, if available
        ProjectUnderstanding understanding = projectUnderstandingRepository.findProjectUnderstandingByProject_Id(projectId);

        String projectGoal = understanding == null ? null : understanding.getProjectGoal();

        // Retrieve all outputs belonging to this project
        List<ProjectOutput> projectOutputs = projectOutputRepository.findAllByProject_Id(projectId);

        // Convert outputs into simple DTOs
        List<ProjectContextDTO.OutputInfo> outputs = projectOutputs.stream().map(output -> new ProjectContextDTO.OutputInfo(output.getId(), output.getTitle(), output.getOutputType(), output.getRepresentationType().name())).toList();


        // Collect game-related information from all project outputs
        GameContextDTO game = gameContextService.buildGameContext(projectOutputs);

        // Collect creative information across all project outputs
        CreativityContextDTO creativity = creativityContextService.buildCreativityContext(projectOutputs);


        // Return the project's basic context
        return new ProjectContextDTO(project.getId(), project.getName(), project.getDescription(), project.getDomain(), project.getStatus().name(), projectGoal, outputs, creativity, game);


    }
}
