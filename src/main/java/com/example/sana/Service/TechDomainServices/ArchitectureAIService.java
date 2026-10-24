package com.example.sana.Service.TechDomainServices;

import com.example.sana.API.ApiConflictException;
import com.example.sana.Config.N8nProperties;
import com.example.sana.Enums.AIDispatchStatus;
import com.example.sana.DTO.IntegrationDTOs.N8nCallbackDTO;
import com.example.sana.Service.IntegrationServices.AIJson;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import com.example.sana.API.ApiException;
import com.example.sana.DTO.TechDomainDTOs.*;
import com.example.sana.Enums.ExecutionStatus;
import com.example.sana.Enums.TechDomainEnums.ArchitectureAITask;
import com.example.sana.Model.*;
import com.example.sana.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArchitectureAIService {

    private final AIInteractionRepository aiInteractionRepository;
    private final ProjectRepository projectRepository;
    private final ProjectOutputRepository projectOutputRepository;
    private final ProjectUnderstandingRepository projectUnderstandingRepository;
    private final ArchitectureGraphMapper architectureGraphMapper;
    private final JsonMapper jsonMapper;
    private final N8nProperties n8nProperties;
    private final ArchitectureGraphValidator graphValidator;
    private final ChangeProposalRepository proposalRepository;
    private final AIJson aiJson;

    // Lock order: Project -> ProjectOutput. No external calls occur in this transaction.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ArchitectureAIRunResponseDTO start(Integer projectId, ArchitectureAIRequestDTO request) {
        return prepare(projectId, request, "TECH", Map.of());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ArchitectureAIRunResponseDTO startBusiness(Integer projectId, ArchitectureAIRequestDTO request, Map<String, Object> businessContext) {
        if (!n8nProperties.isBusinessEnabled()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Business n8n workflow is not enabled yet");
        if (request.getTask() != ArchitectureAITask.ADVISE) throw new ApiException("Business migration currently supports advice only");
        return prepare(projectId, request, "BUSINESS", businessContext);
    }

    private ArchitectureAIRunResponseDTO prepare(Integer projectId, ArchitectureAIRequestDTO request,
                                                String expectedDomain, Map<String, Object> extraContext) {
        if (!n8nProperties.isEnabled()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "n8n integration is disabled");
        validateRequest(request);
        if (projectId == null || projectId <= 0) throw new ApiException("Valid project ID is required");
        Project project = projectRepository.findByIdForUpdate(projectId)
                .orElseThrow(() -> new ApiException("Project not found"));
        User user = project.getOwner();
        if (user == null) throw new ApiException("Project owner is required to record the AI run");
        if (!expectedDomain.equals(project.getDomain())) throw new ApiException("Request domain does not match the project");

        GraphDefinitionDTO graph = new GraphDefinitionDTO(List.of(), List.of(), new LinkedHashMap<>());
        String diagramType = "ARCHITECTURE";
        Integer revision = null;
        List<String> selected = request.getSelectedElementIds() == null ? List.of() : List.copyOf(request.getSelectedElementIds());
        if (request.getTask() != ArchitectureAITask.CREATE) {
            ProjectOutput output = projectOutputRepository.findByIdForUpdate(request.getOutputId())
                    .orElseThrow(() -> new ApiException("Project output not found"));
            if (output.getProject() == null || !projectId.equals(output.getProject().getId())) {
                throw new ApiException("Output does not belong to this project");
            }
            if (output.getGraphRevision() == null) throw new ApiException("Graph revision migration is required");
            if (request.getBaseRevision() != null && !request.getBaseRevision().equals(output.getGraphRevision())) {
                throw new ApiConflictException("Architecture diagram changed. Reload the graph before requesting AI assistance");
            }
            if ("TECH".equals(expectedDomain)) {
                graph = architectureGraphMapper.toDTO(output);
                graphValidator.validate(graph, graph, output.getTitle());
            } else {
                if (output.getRepresentationType() != com.example.sana.Enums.RepresentationType.GRAPH) throw new ApiException("Business advice requires a graph output");
                graph = architectureGraphMapper.toGraphDTO(output);
            }
            diagramType = output.getOutputType();
            revision = output.getGraphRevision();
            var available = new HashSet<String>();
            graph.getNodes().forEach(node -> available.add(node.getKey()));
            graph.getEdges().forEach(edge -> available.add(edge.getKey()));
            if (!available.containsAll(selected)) throw new ApiException("Selected elements must belong to this Architecture Diagram");
        }

        if (request.getDiagramType() != null && !diagramType.equals(request.getDiagramType())) throw new ApiException("Diagram type does not match the selected output");
        Map<String, Object> context = buildContext(project);
        if ("TECH".equals(expectedDomain)) {
            context.put("supportedNodeTypes", ArchitectureGraphRules.NODE_TYPES.stream().sorted().toList());
            context.put("supportedEdgeTypes", ArchitectureGraphRules.EDGE_TYPES.stream().sorted().toList());
            context.put("graphRulesVersion", 1);
            context.put("graphRules", "Node properties.label is required. DATA_FLOW needs a DATABASE or QUEUE endpoint. Edges require distinct endpoints. New IDs must be null.");
        }
        context.putAll(extraContext);
        String requestId = UUID.randomUUID().toString();
        var workflowRequest = new ArchitectureAIWorkflowRequestDTO(1, requestId, expectedDomain, request.getTask(), projectId,
                request.getOutputId(), diagramType, revision, request.getPrompt().trim(), selected,
                context, List.of(), graph);
        AIInteraction run = new AIInteraction();
        run.setRequestId(requestId);
        String storedRequest = jsonMapper.writeValueAsString(workflowRequest);
        if (storedRequest.getBytes(StandardCharsets.UTF_8).length > n8nProperties.getMaxRequestBytes()) throw new ApiException("AI request context is too large");
        run.setRequestJson(storedRequest);
        run.setActionType(request.getTask().name());
        run.setPromptSummary(workflowRequest.getPrompt());
        run.setOutputId(request.getOutputId());
        run.setBaseRevision(revision);
        run.setStatus(ExecutionStatus.PENDING);
        run.setDispatchStatus(AIDispatchStatus.QUEUED); run.setDispatchAttempts(0);
        run.setNextDispatchAt(LocalDateTime.now());
        run.setDeadlineAt(LocalDateTime.now().plusSeconds(n8nProperties.getRequestTimeoutSeconds()));
        run.setProject(project);
        run.setUser(user);
        // modelName and responseJson remain null until a real execution produces them.
        return toResponse(aiInteractionRepository.save(run));
    }

    @Transactional(readOnly = true)
    public ArchitectureAIRunResponseDTO getRun(Integer projectId, UUID requestId) {
        if (projectId == null || projectId <= 0 || requestId == null) throw new ApiException("Project and request IDs are required");
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new ApiException("Project not found"));
        AIInteraction run = aiInteractionRepository.findByRequestIdAndProject_Id(requestId.toString(), projectId)
                .orElseThrow(() -> new ApiException("AI run not found"));
        return toResponse(run);
    }

    private void validateRequest(ArchitectureAIRequestDTO request) {
        if (request == null || request.getTask() == null || request.getPrompt() == null
                || request.getPrompt().isBlank() || request.getPrompt().length() > 10000) {
            throw new ApiException("A valid Architecture AI task and prompt are required");
        }
        List<String> selected = request.getSelectedElementIds() == null ? List.of() : request.getSelectedElementIds();
        if (selected.size() > 100 || selected.stream().anyMatch(key -> key == null || !key.matches("(node|edge):[1-9][0-9]*"))
                || new HashSet<>(selected).size() != selected.size()) {
            throw new ApiException("Select at most 100 unique node:<id> or edge:<id> elements");
        }
        if (request.getTask() == ArchitectureAITask.CREATE) {
            if (request.getOutputId() != null || request.getBaseRevision() != null || !selected.isEmpty()) {
                throw new ApiException("CREATE must not target an existing output, revision, or selection");
            }
        } else if (request.getOutputId() == null || request.getOutputId() <= 0
                || (request.getBaseRevision() != null && request.getBaseRevision() < 0)) {
            throw new ApiException("EDIT and ADVISE require an output ID and, if supplied, a non-negative base revision");
        }
    }

    public Map<String, Object> buildContext(Project project) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("name", project.getName());
        context.put("description", project.getDescription());
        ProjectUnderstanding understanding = projectUnderstandingRepository.findProjectUnderstandingByProject_Id(project.getId());
        if (understanding != null) {
            context.put("goal", understanding.getProjectGoal());
            context.put("actors", readContextJson(understanding.getActorsJson(), "actors"));
            context.put("components", readContextJson(understanding.getComponentsJson(), "components"));
            context.put("constraints", readContextJson(understanding.getConstraintsJson(), "constraints"));
        }
        return context;
    }

    private Object readContextJson(String value, String field) {
        if (value == null || value.isBlank()) return null;
        try {
            return jsonMapper.readerFor(Object.class)
                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                    .readValue(value);
        } catch (JacksonException e) {
            throw new ApiException("Stored project " + field + " contains invalid JSON");
        }
    }

    private ArchitectureAIRunResponseDTO toResponse(AIInteraction run) {
        var response = new ArchitectureAIRunResponseDTO(run.getId(), run.getRequestId(), run.getProject().getId(),
                run.getOutputId(), run.getBaseRevision(), run.getActionType(), run.getStatus(), run.getModelName(), run.getCreatedAt());
        response.setStatusUrl("/api/projects/" + run.getProject().getId() + "/ai/requests/" + run.getRequestId());
        response.setExecutionId(run.getExecutionId()); response.setDispatchStatus(run.getDispatchStatus());
        response.setAcceptedAt(run.getAcceptedAt()); response.setCompletedAt(run.getCompletedAt()); response.setDeadlineAt(run.getDeadlineAt());
        response.setProposalId(run.getProposalId());
        if (run.getProposalId() != null) proposalRepository.findById(run.getProposalId()).ifPresent(proposal ->
                response.setProposalReviewStatus(proposal.getStatus() == com.example.sana.Enums.ProposalStatus.APPROVED ? "ACCEPTED" : proposal.getStatus().name()));
        if (run.getStatus() == ExecutionStatus.COMPLETED && run.getResponseJson() != null) response.setResult(aiJson.read(run.getResponseJson(), N8nCallbackDTO.Result.class));
        if (run.getErrorCode() != null && (run.getStatus() == ExecutionStatus.FAILED || run.getStatus() == ExecutionStatus.TIMED_OUT))
            response.setError(new N8nCallbackDTO.Failure(run.getErrorCode(), run.getErrorMessage(), List.of()));
        return response;
    }
}
