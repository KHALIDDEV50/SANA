package com.example.sana.Repository.CreativityDomainRepository;

import com.example.sana.Model.CreativityDomainModels.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationRepository extends JpaRepository<Location, Integer> {

    Location findLocationById(Integer id);


    // Search locations by name, type or narrative significance
    @Query("""
            SELECT l FROM Location l
            JOIN l.projectNode node
            WHERE node.projectOutput.id = :outputId
              AND (
                  LOWER(l.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(l.locationType) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(l.narrativeSignificance) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    List<Location> searchInOutput(
            @Param("outputId") Integer outputId,
            @Param("keyword") String keyword);


}

