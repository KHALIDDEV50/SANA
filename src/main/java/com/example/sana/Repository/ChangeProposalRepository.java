package com.example.sana.Repository;

import com.example.sana.Model.ChangeProposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChangeProposalRepository extends JpaRepository<ChangeProposal, Integer> {

    List<ChangeProposal> findByAiInteractionId(Integer aiInteractionId);
}
