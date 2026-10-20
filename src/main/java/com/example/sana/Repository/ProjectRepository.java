package com.example.sana.Repository;

import com.example.sana.Model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Project findProjectById(Long id);

    List<Project> findAllByOwner_Id(Long ownerId);
}