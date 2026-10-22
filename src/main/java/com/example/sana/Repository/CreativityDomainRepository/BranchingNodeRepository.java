package com.example.sana.Repository.CreativityDomainRepository;

import com.example.sana.Model.CreativityDomainModels.BranchingNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchingNodeRepository extends JpaRepository<BranchingNode, Integer> {

    // Get all branching scenes inside a specific output
    List<BranchingNode> findByProjectNode_ProjectOutput_IdOrderByIdAsc(
            Integer outputId);

    // Get one branching scene within the selected output
    BranchingNode findByIdAndProjectNode_ProjectOutput_Id(
            Integer nodeId, Integer outputId);

    // Find the branching scene connected to a ProjectNode
    BranchingNode findByProjectNode_Id(Integer projectNodeId);
}
