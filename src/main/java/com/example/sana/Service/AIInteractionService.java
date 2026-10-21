package com.example.sana.Service;

import com.example.sana.API.ApiException;
import com.example.sana.Model.AIInteraction;
import com.example.sana.Model.ChangeProposal;
import com.example.sana.Model.Project;
import com.example.sana.Model.User;
import com.example.sana.Repository.AIInteractionRepository;
import com.example.sana.Repository.ChangeProposalRepository;
import com.example.sana.Repository.ProjectRepository;
import com.example.sana.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AIInteractionService {

    private final AIInteractionRepository aiInteractionRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ChangeProposalRepository changeProposalRepository;

    public List<AIInteraction> getAllAIInteractions() {
        return aiInteractionRepository.findAll();
    }

    public AIInteraction getAIInteractionById(Integer ai_interaction_id) {
        return aiInteractionRepository.findById(ai_interaction_id)
                .orElseThrow(() -> new ApiException("AI interaction not found"));
    }

    public void addAIInteraction(Integer user_id, Integer project_id, AIInteraction aiInteraction) {
        User user = userRepository.findById(user_id)
                .orElseThrow(() -> new ApiException("User not found"));
        Project project = projectRepository.findById(project_id)
                .orElseThrow(() -> new ApiException("Project not found"));

        aiInteraction.setId(null);
        aiInteraction.setCreatedAt(null);
        aiInteraction.setUser(user);
        aiInteraction.setProject(project);
        aiInteractionRepository.save(aiInteraction);
    }

    public void updateAIInteraction(Integer ai_interaction_id, AIInteraction aiInteraction) {
        AIInteraction oldAIInteraction = aiInteractionRepository.findById(ai_interaction_id)
                .orElseThrow(() -> new ApiException("AI interaction not found"));

        oldAIInteraction.setActionType(aiInteraction.getActionType());
        oldAIInteraction.setPromptSummary(aiInteraction.getPromptSummary());
        oldAIInteraction.setResponseJson(aiInteraction.getResponseJson());
        oldAIInteraction.setModelName(aiInteraction.getModelName());
        oldAIInteraction.setStatus(aiInteraction.getStatus());

        aiInteractionRepository.save(oldAIInteraction);
    }

    @Transactional
    public void deleteAIInteraction(Integer ai_interaction_id) {
        AIInteraction aiInteraction = aiInteractionRepository.findById(ai_interaction_id)
                .orElseThrow(() -> new ApiException("AI interaction not found"));

        List<ChangeProposal> proposals = changeProposalRepository.findByAiInteractionId(ai_interaction_id);
        for (ChangeProposal proposal : proposals) {
            proposal.setAiInteraction(null);
        }
        changeProposalRepository.saveAll(proposals);
        changeProposalRepository.flush();

        aiInteractionRepository.delete(aiInteraction);
    }
}
