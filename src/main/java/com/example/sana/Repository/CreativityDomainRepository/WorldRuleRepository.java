package com.example.sana.Repository.CreativityDomainRepository;


import com.example.sana.Model.CreativityDomainModels.WorldRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorldRuleRepository extends JpaRepository<WorldRule, Integer> {

    WorldRule findWorldRuleById(Integer id);



    // Search world rules by name, description or scope
    @Query("""
            SELECT r FROM WorldRule r
            JOIN r.projectNode node
            WHERE node.projectOutput.id = :outputId
              AND (
                  LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(r.scope) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    List<WorldRule> searchInOutput(
            @Param("outputId") Integer outputId,
            @Param("keyword") String keyword);


}

