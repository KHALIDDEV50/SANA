package com.example.sana.AI.Service;

import com.example.sana.AI.DTO.*;
import com.example.sana.AI.Enums.*;
import com.example.sana.API.ApiException;

import com.example.sana.Model.ProjectNode;
import com.example.sana.Model.ProjectOutput;

import com.example.sana.Repository.ProjectNodeRepository;
import com.example.sana.Repository.ProjectOutputRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SanaAIRequestService {

    private final AIContextSelectionService aiContextSelectionService;
    private final ProjectOutputRepository projectOutputRepository;
    private final ProjectNodeRepository projectNodeRepository;


    // Validate the request and prepare project context for AI
    // All IDs are received from the API path
    @Transactional(readOnly = true)
    public SanaAIPreparedRequestDTO prepareRequest(Integer projectId, Integer outputId, Integer selectedNodeId, SanaAIRequestDTO request) {

        // Validate the project ID
        if (projectId == null || projectId <= 0) {
            throw new ApiException("Valid project ID is required");
        }

        // Validate the request body
        if (request == null || request.getMode() == null || request.getTask() == null || request.getMessage() == null || request.getMessage().isBlank()) {
            throw new ApiException("Invalid AI request");
        }

        if (request.getMessage().length() > 10000) {
            throw new ApiException("AI message is too long");
        }

        // Validate mode and task compatibility
        validateModeAndTask(request.getMode(), request.getTask(), outputId, selectedNodeId);

        // Validate optional output and node IDs
        validateSelection(projectId, outputId, selectedNodeId);

        // Determine which project information the AI needs
        AIContextScope scope = resolveContextScope(request.getTask());

        // These scopes do not support selecting a single node
        if (selectedNodeId != null && (scope == AIContextScope.OVERVIEW || scope == AIContextScope.CHARACTER_BALANCE)) {

            throw new ApiException("Selected node is not supported for this AI task");
        }

        // Build the relevant context using the existing service
        ProjectContextDTO context = aiContextSelectionService.selectContext(projectId, scope, selectedNodeId);

        // Return the prepared request without calling n8n or saving
        return new SanaAIPreparedRequestDTO(projectId, request.getMode(), request.getTask(), request.getMessage().trim(), outputId, selectedNodeId, scope, context);
    }


    // Validate combinations of AI mode and task
    private void validateModeAndTask(AIMode mode, AITaskType task, Integer outputId, Integer selectedNodeId) {

        if (mode == AIMode.IDEA_CREATOR) {

            if (task != AITaskType.IDEA_BLUEPRINT) {
                throw new ApiException("Idea Creator only supports blueprint generation");
            }

            // Idea Creator starts without targeting a specific element
            if (outputId != null || selectedNodeId != null) {
                throw new ApiException("Idea Creator cannot target an output or node");
            }

        } else if (mode == AIMode.CREATIVE_ASSISTANT) {

            if (task == AITaskType.IDEA_BLUEPRINT) {
                throw new ApiException("Use Idea Creator for blueprint generation");
            }
        }
    }


    // Validate the output and node belong to the selected project
    private void validateSelection(Integer projectId, Integer outputId, Integer selectedNodeId) {

        // A node must always be selected through its output
        if (selectedNodeId != null && outputId == null) {
            throw new ApiException("Output ID is required when selecting a node");
        }

        // No specific selection: use project-level context
        if (outputId == null) {
            return;
        }

        if (outputId <= 0) {
            throw new ApiException("Invalid output ID");
        }

        ProjectOutput output = projectOutputRepository.findProjectOutputById(outputId);

        if (output == null || output.getProject() == null || !projectId.equals(output.getProject().getId())) {

            throw new ApiException("Selected output does not belong to this project");
        }

        // An output can be selected without selecting a node
        if (selectedNodeId == null) {
            return;
        }

        if (selectedNodeId <= 0) {
            throw new ApiException("Invalid node ID");
        }

        ProjectNode node = projectNodeRepository.findProjectNodeById(selectedNodeId);

        if (node == null || node.getProjectOutput() == null || !outputId.equals(node.getProjectOutput().getId())) {

            throw new ApiException("Selected node does not belong to this output");
        }
    }


    // Choose the appropriate project context for each AI task
    private AIContextScope resolveContextScope(AITaskType task) {

        return switch (task) {

            case IDEA_BLUEPRINT ->
                    AIContextScope.OVERVIEW;

            case GENERAL_ASSISTANCE ->
                    AIContextScope.FULL_PROJECT;

            case CHARACTER_DEVELOPMENT ->
                    AIContextScope.CHARACTER;

            case WORLD_BUILDING ->
                    AIContextScope.WORLD_BUILDING;

            case PLOT_DEVELOPMENT ->
                    AIContextScope.STORY_PLOT;

            case GAME_NARRATIVE ->
                    AIContextScope.GAME_NARRATIVE;

            case CHARACTER_BALANCE ->
                    AIContextScope.CHARACTER_BALANCE;

            case CONSISTENCY_CHECK ->
                    AIContextScope.FULL_PROJECT;
        };
    }
}
