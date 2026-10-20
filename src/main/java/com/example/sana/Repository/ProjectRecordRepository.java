package com.example.sana.Repository;

import com.example.sana.Model.ProjectRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRecordRepository extends JpaRepository<ProjectRecord, Integer> {
    ProjectRecord findProjectRecordById(Integer id);
}
