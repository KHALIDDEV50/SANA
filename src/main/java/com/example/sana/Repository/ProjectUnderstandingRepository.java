package com.example.sana.Repository;

import com.example.sana.Model.ProjectUnderstanding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectUnderstandingRepository
        extends JpaRepository<ProjectUnderstanding, Long> {

    ProjectUnderstanding findProjectUnderstandingById(Long id);

    ProjectUnderstanding findProjectUnderstandingByProject_Id(Long projectId);
}