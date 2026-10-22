package com.example.sana.Repository.CreativityDomainRepository;


import com.example.sana.Model.CreativityDomainModels.CharacterProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharacterProfileRepository extends JpaRepository<CharacterProfile, Integer> {

    CharacterProfile findCharacterProfileById(Integer id);


    // Search characters by name, description, aliases or role
    // Only return characters inside the selected ProjectOutput
    @Query("""
            SELECT c FROM CharacterProfile c
            JOIN c.projectNode node
            WHERE node.projectOutput.id = :outputId
              AND (
                  LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(c.shortDescription) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(c.aliases) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(c.role) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    List<CharacterProfile> searchInOutput(
            @Param("outputId") Integer outputId,
            @Param("keyword") String keyword);



}

