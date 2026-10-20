package com.example.sana.Repository;

import com.example.sana.Model.ProjectNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectNodeRepository extends JpaRepository<ProjectNode, Integer> {

    ProjectNode findProjectNodeById(Integer id);

}
