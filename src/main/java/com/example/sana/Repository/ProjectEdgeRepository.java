package com.example.sana.Repository;

import com.example.sana.Model.ProjectEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectEdgeRepository extends JpaRepository<ProjectEdge, Integer> {
    ProjectEdge findProjectEdgeById(Integer id);
}
