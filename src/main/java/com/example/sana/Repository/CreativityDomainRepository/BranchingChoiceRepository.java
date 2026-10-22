
package com.example.sana.Repository.CreativityDomainRepository;

import com.example.sana.Model.CreativityDomainModels.BranchingChoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchingChoiceRepository extends JpaRepository<BranchingChoice, Integer> {

    // Get all choices inside a ProjectOutput
    List<BranchingChoice> findByProjectEdge_ProjectOutput_IdOrderByIdAsc(
            Integer outputId);

    // Get choices leaving a specific ProjectNode
    List<BranchingChoice> findByProjectEdge_SourceNode_IdOrderByDisplayOrderAscIdAsc(
            Integer projectNodeId);

    // Find a choice inside the selected output
    BranchingChoice findByIdAndProjectEdge_ProjectOutput_Id(
            Integer choiceId, Integer outputId);

    // Check choices connected to a specific ProjectNode
    List<BranchingChoice> findByProjectEdge_SourceNode_Id(
            Integer projectNodeId);

    List<BranchingChoice> findByProjectEdge_TargetNode_Id(
            Integer projectNodeId);


    // Check whether a variable is used in any condition or effect
    boolean existsByConditionVariable_IdOrEffectVariable_Id(
            Integer conditionVariableId,
            Integer effectVariableId);

}
