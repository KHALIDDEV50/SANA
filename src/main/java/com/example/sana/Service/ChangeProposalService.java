package com.example.sana.Service;

import com.example.sana.API.ApiException;
import com.example.sana.Model.AIInteraction;
import com.example.sana.Model.ChangeProposal;
import com.example.sana.Model.ChangeProposalItem;
import com.example.sana.Model.Project;
import com.example.sana.Repository.AIInteractionRepository;
import com.example.sana.Repository.ChangeProposalItemRepository;
import com.example.sana.Repository.ChangeProposalRepository;
import com.example.sana.Repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChangeProposalService {

    private final ChangeProposalRepository changeProposalRepository;
    private final ProjectRepository projectRepository;
    private final AIInteractionRepository aiInteractionRepository;
    private final ChangeProposalItemRepository changeProposalItemRepository;

    public List<ChangeProposal> getAllChangeProposals() {
        return changeProposalRepository.findAll();
    }

    public ChangeProposal getChangeProposalById(Integer proposal_id) {
        return changeProposalRepository.findById(proposal_id)
                .orElseThrow(() -> new ApiException("Change proposal not found"));
    }

    public void addChangeProposal(Integer project_id, Integer ai_interaction_id, ChangeProposal changeProposal) {
        Project project = projectRepository.findById(project_id)
                .orElseThrow(() -> new ApiException("Project not found"));

        AIInteraction aiInteraction = null;
        if (ai_interaction_id != null) {
            aiInteraction = aiInteractionRepository.findById(ai_interaction_id)
                    .orElseThrow(() -> new ApiException("AI interaction not found"));

            if (!aiInteraction.getProject().getId().equals(project_id)) {
                throw new ApiException("AI interaction must belong to the proposal project");
            }
        }

        changeProposal.setId(null);
        changeProposal.setCreatedAt(null);
        changeProposal.setProject(project);
        changeProposal.setAiInteraction(aiInteraction);
        changeProposal.setItems(new HashSet<>());
        changeProposalRepository.save(changeProposal);
    }

    public void updateChangeProposal(Integer proposal_id, ChangeProposal changeProposal) {
        ChangeProposal oldChangeProposal = changeProposalRepository.findById(proposal_id)
                .orElseThrow(() -> new ApiException("Change proposal not found"));

        oldChangeProposal.setTitle(changeProposal.getTitle());
        oldChangeProposal.setDescription(changeProposal.getDescription());
        oldChangeProposal.setStatus(changeProposal.getStatus());
        oldChangeProposal.setBaseVersion(changeProposal.getBaseVersion());
        oldChangeProposal.setReviewedAt(changeProposal.getReviewedAt());

        changeProposalRepository.save(oldChangeProposal);
    }

    @Transactional
    public void deleteChangeProposal(Integer proposal_id) {
        ChangeProposal changeProposal = changeProposalRepository.findById(proposal_id)
                .orElseThrow(() -> new ApiException("Change proposal not found"));

        List<ChangeProposalItem> items = changeProposalItemRepository.findByChangeProposalId(proposal_id);
        changeProposalItemRepository.deleteAll(items);
        changeProposalItemRepository.flush();

        changeProposalRepository.delete(changeProposal);
    }
}
