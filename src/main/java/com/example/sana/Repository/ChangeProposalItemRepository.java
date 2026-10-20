package com.example.sana.Repository;

import com.example.sana.Model.ChangeProposalItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChangeProposalItemRepository extends JpaRepository<ChangeProposalItem, Integer> {

    List<ChangeProposalItem> findByChangeProposalId(Integer proposalId);
}
