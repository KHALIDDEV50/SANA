package com.example.sana.Service.CreativityDomainServices;

import com.example.sana.API.ApiException;
import com.example.sana.Config.N8nProperties;
import com.example.sana.DTO.AIDTOs.AIRequestDTO;
import com.example.sana.DTO.AIDTOs.AIRunResponseDTO;
import com.example.sana.DTO.AIDTOs.AIWorkflowRequestDTO;
import com.example.sana.DTO.CreativityDomainDTOs.CreativityAIRequestDTO;
import com.example.sana.Model.AIInteraction;
import com.example.sana.Model.Project;
import com.example.sana.Repository.AIInteractionRepository;
import com.example.sana.Repository.ProjectRepository;
import com.example.sana.Service.AIServices.AIRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Augments only CREATIVITY requests with richer, read-only project references.
 * No change to shared AI DTOs, dispatch workers, callbacks, or proposal acceptance.
 * The enclosing transaction ensures dispatch cannot see a half-prepared request.
 */
@Service
@RequiredArgsConstructor
public class CreativityAIRequestService {
    private final ProjectRepository projects;
    private final AIInteractionRepository interactions;
    private final AIRequestService sharedRequests;
    private final CreativityAIContextService creativityContext;
    private final N8nProperties n8nProperties;
    private final JsonMapper mapper;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AIRunResponseDTO start(Integer projectId, CreativityAIRequestDTO request) {
        // Do NOT trust a domain sent by the browser.
        Project project = projects.findById(projectId)
                .orElseThrow(() -> new ApiException("Project not found"));
        if (!"CREATIVITY".equals(project.getDomain())) {
            throw new ApiException("This endpoint is only for Creativity projects");
        }

        AIRequestDTO standard = new AIRequestDTO();
        standard.setTask(request.getTask());
        standard.setPrompt(request.getPrompt());
        standard.setDiagramType(request.getDiagramType());
        standard.setOutputId(request.getOutputId());
        standard.setBaseRevision(request.getBaseRevision());
        standard.setSelectedElementIds(request.getSelectedElementIds());

        // Spring joins this transaction. The worker sees QUEUED only after commit.
        AIRunResponseDTO run = sharedRequests.start(projectId, standard);
        AIInteraction stored = interactions.findByRequestIdAndProject_Id(run.getRequestId(), projectId)
                .orElseThrow(() -> new ApiException("New AI request not found"));
        if (stored.getRequestJson() == null) {
            throw new ApiException("AI request snapshot was not saved");
        }

        AIWorkflowRequestDTO snapshot = mapper.readValue(stored.getRequestJson(), AIWorkflowRequestDTO.class);
        Map<String, Object> context = new LinkedHashMap<>(snapshot.getContext());
        context.put("creativity", creativityContext.build(
                project, snapshot.getOutputId(), snapshot.getSelectedElementIds()));
        snapshot.setContext(context);
        snapshot.setConversationHistory(sanitizeHistory(request.getConversationHistory()));

        String json = mapper.writeValueAsString(snapshot);
        if (json.getBytes(StandardCharsets.UTF_8).length > n8nProperties.getMaxRequestBytes()) {
            throw new ApiException("Creativity AI context is too large");
        }
        stored.setRequestJson(json);
        interactions.saveAndFlush(stored);
        return run;
    }

    private List<Object> sanitizeHistory(List<CreativityAIRequestDTO.ConversationTurn> turns) {
        if (turns == null || turns.isEmpty()) return List.of();
        if (turns.size() > 12) throw new ApiException("Conversation history exceeds 12 turns");
        List<Object> result = new ArrayList<>();
        for (CreativityAIRequestDTO.ConversationTurn turn : turns) {
            if (turn == null || turn.role() == null || turn.content() == null ||
                    !("user".equals(turn.role()) || "assistant".equals(turn.role())) ||
                    turn.content().isBlank() || turn.content().length() > 1500) {
                throw new ApiException("Invalid Creativity conversation turn");
            }
            result.add(Map.of("role", turn.role(), "content", turn.content().trim()));
        }
        return result;
    }
}
