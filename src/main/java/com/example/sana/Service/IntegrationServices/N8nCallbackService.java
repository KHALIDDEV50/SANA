package com.example.sana.Service.IntegrationServices;

import com.example.sana.API.*;
import com.example.sana.DTO.IntegrationDTOs.N8nCallbackDTO;
import com.example.sana.DTO.TechDomainDTOs.*;
import com.example.sana.Enums.*;
import com.example.sana.Model.*;
import com.example.sana.Repository.*;
import com.example.sana.Service.TechDomainServices.*;
import com.example.sana.Service.Business.BusinessResponseValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class N8nCallbackService {
    private final AIInteractionRepository interactions;
    private final ChangeProposalRepository proposals;
    private final ChangeProposalItemRepository items;
    private final ArchitectureGraphValidator validator;
    private final ArchitectureGraphDiff diff;
    private final BusinessResponseValidator businessValidator;
    private final AIJson json;

    // false means a late callback was rejected after preserving a timed-out state.
    @Transactional
    public boolean receive(N8nCallbackDTO callback) {
        validateEnvelope(callback);
        AIInteraction run = interactions.findByRequestIdForUpdate(callback.requestId())
                .orElseThrow(() -> new ApiException("AI request not found"));
        if (run.getRequestJson() == null) throw new ApiException("Request has no authoritative snapshot");
        var request = json.read(run.getRequestJson(), ArchitectureAIWorkflowRequestDTO.class);
        if (!run.getRequestId().equals(request.getRequestId()) || !run.getProject().getId().equals(request.getProjectId())
                || !Objects.equals(run.getOutputId(), request.getOutputId()) || !Objects.equals(run.getBaseRevision(), request.getBaseRevision())
                || !run.getActionType().equals(request.getTask().name())) throw new ApiException("AI request metadata is inconsistent");
        validateResult(callback, request);
        String hash = json.hash(callback);
        if (AIRunLifecycle.terminal(run)) {
            if (hash.equals(run.getCallbackHash())) return true;
            if (run.getStatus() == ExecutionStatus.TIMED_OUT) return false;
            throw new ApiConflictException("A different terminal result is already recorded");
        }
        if (run.getExecutionId() != null && !run.getExecutionId().equals(callback.executionId()))
            throw new ApiConflictException("Execution ID does not match the stored request");
        if (run.getDeadlineAt() != null && LocalDateTime.now().isAfter(run.getDeadlineAt())) {
            AIRunLifecycle.fail(run, ExecutionStatus.TIMED_OUT, "CALLBACK_TIMEOUT", "AI request timed out. Submit a new request to retry.");
            return false;
        }
        run.setExecutionId(callback.executionId());
        run.setCallbackHash(hash);
        run.setCompletedAt(LocalDateTime.now());
        run.setDispatchStatus(AIDispatchStatus.TERMINAL);
        run.setDispatchLeaseUntil(null); run.setNextDispatchAt(null);
        if (callback.status() == N8nCallbackDTO.CallbackStatus.FAILED) {
            // Raw provider/tool error text can contain credentials; do not persist or expose it.
            String code = Set.of("INVALID_AI_RESULT", "WORKFLOW_FAILED", "MODEL_UNAVAILABLE", "TOOL_FAILED", "EXECUTION_TIMEOUT").contains(callback.error().code())
                    ? callback.error().code() : "WORKFLOW_FAILED";
            AIRunLifecycle.fail(run, ExecutionStatus.FAILED, code, "AI workflow failed. Submit a new request to retry.");
        } else {
            run.setStatus(ExecutionStatus.COMPLETED);
            run.setModelName(callback.result().modelName());
            run.setResponseJson(json.write(callback.result()));
            if (callback.result().type() == N8nCallbackDTO.ResultType.PROPOSAL) storeProposal(run, request, callback.result().proposal());
        }
        return true;
    }

    private void validateEnvelope(N8nCallbackDTO callback) {
        if (callback == null || !Integer.valueOf(1).equals(callback.schemaVersion()) || callback.status() == null)
            throw new ApiException("Unsupported callback schema or status");
        try { if (!UUID.fromString(callback.requestId()).toString().equals(callback.requestId())) throw new IllegalArgumentException(); }
        catch (RuntimeException e) { throw new ApiException("Callback requestId must be a canonical UUID"); }
        ArchitectureGraphRules.text(callback.executionId(), 255, "Execution ID");
        if (callback.status() == N8nCallbackDTO.CallbackStatus.COMPLETED) {
            if (callback.result() == null || callback.error() != null) throw new ApiException("COMPLETED requires result and excludes error");
        } else {
            if (callback.error() == null || callback.result() != null) throw new ApiException("FAILED requires error and excludes result");
            ArchitectureGraphRules.text(callback.error().message(), 2000, "Failure message");
            if (callback.error().code() == null || !callback.error().code().matches("[A-Z][A-Z0-9_]{0,63}")) throw new ApiException("Invalid failure code");
            if (callback.error().details() != null) {
                if (callback.error().details().size() > 20) throw new ApiException("Too many failure details");
                callback.error().details().forEach(detail -> ArchitectureGraphRules.text(detail, 1000, "Failure detail"));
            }
        }
    }

    private void validateResult(N8nCallbackDTO callback, ArchitectureAIWorkflowRequestDTO request) {
        if (callback.status() == N8nCallbackDTO.CallbackStatus.FAILED) return;
        var result = callback.result();
        if (result.type() == null) throw new ApiException("Result type is required");
        ArchitectureGraphRules.text(result.message(), 20000, "AI message");
        ArchitectureGraphRules.text(result.modelName(), 255, "Model name");
        switch (result.type()) {
            case ADVICE -> {
                if (result.proposal() != null || result.clarification() != null) throw new ApiException("ADVICE excludes proposal and clarification");
                if ("BUSINESS".equals(request.getDomain())) {
                    Object tool = request.getContext().get("businessToolType");
                    try { businessValidator.validate(result.message(), BusinessToolType.valueOf(String.valueOf(tool))); }
                    catch (RuntimeException e) { throw new ApiException("Invalid structured business analysis result"); }
                }
            }
            case CLARIFICATION -> {
                if (result.proposal() != null || result.clarification() == null || result.clarification().suggestedAnswers() == null)
                    throw new ApiException("CLARIFICATION requires a question and suggestedAnswers array");
                ArchitectureGraphRules.text(result.clarification().question(), 2000, "Clarification question");
                if (result.clarification().suggestedAnswers().size() > 10) throw new ApiException("Too many suggested answers");
                result.clarification().suggestedAnswers().forEach(answer -> ArchitectureGraphRules.text(answer, 255, "Suggested answer"));
            }
            case PROPOSAL -> {
                if (!"TECH".equals(request.getDomain()) || !"ARCHITECTURE".equals(request.getDiagramType()) || request.getTask().name().equals("ADVISE"))
                    throw new ApiException("This request does not permit graph proposals");
                if (result.clarification() != null || result.proposal() == null) throw new ApiException("PROPOSAL requires a graph and excludes clarification");
                ArchitectureGraphRules.text(result.proposal().title(), 255, "Proposal title");
                ArchitectureGraphRules.text(result.proposal().description(), 10000, "Proposal description");
                String title = request.getOutputId() == null ? result.proposal().title() : String.valueOf(request.getCurrentGraph().getProperties().get("title"));
                validator.validate(result.proposal().proposedGraph(), request.getOutputId() == null ? null : request.getCurrentGraph(), title);
            }
        }
    }

    private void storeProposal(AIInteraction run, ArchitectureAIWorkflowRequestDTO request, N8nCallbackDTO.Proposal candidate) {
        var proposal = new ChangeProposal();
        proposal.setManagedRequestId(run.getRequestId()); proposal.setTitle(candidate.title()); proposal.setDescription(candidate.description());
        proposal.setStatus(ProposalStatus.PENDING); proposal.setBaseVersion(run.getProject().getVersion());
        proposal.setBaseRevision(run.getBaseRevision()); proposal.setOutputId(run.getOutputId());
        proposal.setProject(run.getProject()); proposal.setAiInteraction(run); proposal.setProposedGraphJson(json.write(candidate.proposedGraph()));
        proposals.save(proposal);
        for (var change : diff.compute(request.getOutputId() == null ? null : request.getCurrentGraph(), candidate.proposedGraph(), run.getOutputId())) {
            change.setChangeProposal(proposal); items.save(change);
        }
        run.setProposalId(proposal.getId());
    }
}
