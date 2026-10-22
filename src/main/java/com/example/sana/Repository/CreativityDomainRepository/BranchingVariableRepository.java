package com.example.sana.Repository.CreativityDomainRepository;

import com.example.sana.Model.CreativityDomainModels.BranchingVariable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchingVariableRepository
        extends JpaRepository<BranchingVariable, Integer> {

    // Get all game variables for one ProjectOutput
    List<BranchingVariable> findByProjectOutput_IdOrderByIdAsc(
            Integer outputId);

    // Find a variable inside the selected output
    BranchingVariable findByIdAndProjectOutput_Id(
            Integer variableId, Integer outputId);

    // Prevent duplicate variable names within an output
    boolean existsByProjectOutput_IdAndName(
            Integer outputId, String name);
}
