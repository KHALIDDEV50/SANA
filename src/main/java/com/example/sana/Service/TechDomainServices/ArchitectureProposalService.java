package com.example.sana.Service.TechDomainServices;

import com.example.sana.API.*;
import com.example.sana.DTO.IntegrationDTOs.N8nCallbackDTO;
import com.example.sana.DTO.TechDomainDTOs.*;
import com.example.sana.Enums.*;
import com.example.sana.Model.*;
import com.example.sana.Repository.*;
import com.example.sana.Service.IntegrationServices.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ArchitectureProposalService {
    private final ProjectAccessService access;
    private final ChangeProposalRepository proposals;
    private final ChangeProposalItemRepository items;
    private final AIInteractionRepository interactions;
    private final ProjectOutputRepository outputs;
    private final ArchitectureGraphValidator validator;
    private final ArchitectureGraphApplier applier;
    private final ArchitectureAIService requests;
    private final AIJson json;

    @Transactional(readOnly = true)
    public ArchitectureProposalResponseDTO preview(Integer projectId, Integer proposalId) {
        access.project(projectId, false);
        var proposal = load(projectId, proposalId, false);
        List<ArchitectureProposalResponseDTO.Change> changes = items.findByChangeProposalId(proposalId).stream()
                .sorted(Comparator.comparing(ChangeProposalItem::getId))
                .map(item -> new ArchitectureProposalResponseDTO.Change(item.getOperation(), item.getTargetType(), item.getTargetId(),
                        item.getBeforeJson() == null ? null : json.read(item.getBeforeJson(), Object.class),
                        item.getAfterJson() == null ? null : json.read(item.getAfterJson(), Object.class))).toList();
        return new ArchitectureProposalResponseDTO(proposalId, proposal.getManagedRequestId(), proposal.getTitle(), proposal.getDescription(),
                proposal.getOutputId(), proposal.getBaseRevision(), reviewStatus(proposal), proposal.getAppliedOutputId(),
                json.read(proposal.getProposedGraphJson(), GraphDefinitionDTO.class), changes, proposal.getCreatedAt(), proposal.getReviewedAt());
    }

    // All canonical writers use the same output lock. No model/network calls are made here.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ArchitectureAcceptanceDTO accept(Integer projectId, Integer proposalId) {
        Project project = access.project(projectId, true);
        var initial = load(projectId, proposalId, false);
        ProjectOutput output = null;
        if (initial.getStatus() == ProposalStatus.PENDING && initial.getOutputId() != null)
            output = outputs.findByIdForUpdate(initial.getOutputId()).orElseThrow(() -> new ApiConflictException("Target diagram was deleted"));
        AIInteraction run = interactions.findByRequestIdForUpdate(initial.getManagedRequestId()).orElseThrow();
        ChangeProposal proposal = load(projectId, proposalId, true);
        if (proposal.getStatus() == ProposalStatus.APPROVED) return accepted(proposal);
        if (proposal.getStatus() != ProposalStatus.PENDING) throw new ApiConflictException("Rejected proposals cannot be accepted");
        var request = json.read(run.getRequestJson(), ArchitectureAIWorkflowRequestDTO.class);
        if (run.getStatus() != ExecutionStatus.COMPLETED || !"TECH".equals(project.getDomain()) || !"TECH".equals(request.getDomain())
                || !"ARCHITECTURE".equals(request.getDiagramType()) || !projectId.equals(run.getProject().getId())
                || !Objects.equals(run.getOutputId(), proposal.getOutputId()) || !Objects.equals(run.getBaseRevision(), proposal.getBaseRevision()))
            throw new ApiException("Proposal does not match its completed Architecture request");
        if (!contextHash(request.getContext()).equals(contextHash(requests.buildContext(project))))
            throw new ApiConflictException("Project context changed. Request a new proposal");
        GraphDefinitionDTO graph = json.read(proposal.getProposedGraphJson(), GraphDefinitionDTO.class);
        var recordedResult = json.read(run.getResponseJson(), N8nCallbackDTO.Result.class);
        if (recordedResult.proposal() == null || !json.hash(recordedResult.proposal().proposedGraph()).equals(json.hash(graph)))
            throw new ApiConflictException("Stored proposal content does not match the recorded result");
        validator.validate(graph, run.getOutputId() == null ? null : request.getCurrentGraph(), proposal.getTitle());
        if (run.getOutputId() == null) {
            output = new ProjectOutput(); output.setProject(project); output.setTitle((String) graph.getProperties().get("title"));
            output.setOutputType("ARCHITECTURE"); output.setRepresentationType(RepresentationType.GRAPH);
            output.setCreatedAt(LocalDateTime.now()); output.setUpdatedAt(output.getCreatedAt()); outputs.save(output);
        } else if (output == null || !projectId.equals(output.getProject().getId()) || !"ARCHITECTURE".equals(output.getOutputType())
                || output.getRepresentationType() != RepresentationType.GRAPH || !Objects.equals(proposal.getBaseRevision(), output.getGraphRevision())) {
            throw new ApiConflictException("Diagram revision changed. Request a new proposal");
        }
        Map<String, Integer> assignedIds = applier.apply(output, graph);
        proposal.setStatus(ProposalStatus.APPROVED); proposal.setReviewedAt(LocalDateTime.now());
        proposal.setAppliedOutputId(output.getId()); proposal.setAppliedRevision(output.getGraphRevision()); proposal.setAppliedMappingJson(json.write(assignedIds));
        outputs.flush();
        return accepted(proposal);
    }

    @Transactional
    public ArchitectureProposalResponseDTO reject(Integer projectId, Integer proposalId) {
        access.project(projectId, true);
        var initial = load(projectId, proposalId, false);
        interactions.findByRequestIdForUpdate(initial.getManagedRequestId()).orElseThrow();
        var proposal = load(projectId, proposalId, true);
        if (proposal.getStatus() == ProposalStatus.APPROVED) throw new ApiConflictException("Accepted proposals cannot be rejected");
        if (proposal.getStatus() == ProposalStatus.PENDING) {proposal.setStatus(ProposalStatus.REJECTED); proposal.setReviewedAt(LocalDateTime.now());}
        return preview(projectId, proposalId);
    }

    private ChangeProposal load(Integer projectId, Integer id, boolean lock) {
        ChangeProposal proposal = (lock ? proposals.findByIdForUpdate(id) : proposals.findById(id))
                .orElseThrow(() -> new ApiException("Proposal not found"));
        if (proposal.getManagedRequestId() == null || !projectId.equals(proposal.getProject().getId())) throw new ApiException("Proposal not found in this project");
        return proposal;
    }

    private String contextHash(Map<String, Object> context) {
        Map<String, Object> facts = new LinkedHashMap<>();
        for (String key : List.of("name", "description", "goal", "actors", "components", "constraints")) if (context.containsKey(key)) facts.put(key, context.get(key));
        return json.hash(facts);
    }

    private ArchitectureAcceptanceDTO accepted(ChangeProposal proposal) {
        Map<String, Integer> mapping = proposal.getAppliedMappingJson() == null ? Map.of() : json.read(proposal.getAppliedMappingJson(), Map.class);
        return new ArchitectureAcceptanceDTO(proposal.getId(), "ACCEPTED", proposal.getAppliedOutputId(), proposal.getAppliedRevision(), mapping);
    }

    public static String reviewStatus(ChangeProposal proposal) { return proposal.getStatus() == ProposalStatus.APPROVED ? "ACCEPTED" : proposal.getStatus().name(); }
}
