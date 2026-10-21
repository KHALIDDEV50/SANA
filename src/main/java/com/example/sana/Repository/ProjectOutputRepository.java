package com.example.sana.Repository;

import com.example.sana.Model.ProjectOutput;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectOutputRepository extends JpaRepository<ProjectOutput, Integer> {

    ProjectOutput findProjectOutputById(Integer id);

    List<ProjectOutput> findAllByProject_Id(Integer projectId);
}
