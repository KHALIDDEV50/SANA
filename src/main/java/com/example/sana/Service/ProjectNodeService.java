package com.example.sana.Service;

import com.example.sana.API.ApiException;
import com.example.sana.Model.Project;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Repository.ProjectNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectNodeService {

    private final ProjectNodeRepository projectNodeRepository;
   // private final ProjectRepository projectRepository;

    public List<ProjectNode> getAllNodes(){
        return projectNodeRepository.findAll();
    }

    public void addNode(Integer projectId, ProjectNode projectNode){
//        Project project = projectRepository.findProjectById(projectId);
//        if (project == null){
//            throw new ApiException("project not found");
//        }
        //projectNode.setProject(project);
        projectNode.setCreatedAt(LocalDate.now());
        projectNodeRepository.save(projectNode);
    }


    public void updateNode(Integer projectNodeId, ProjectNode projectNode){
        ProjectNode node = projectNodeRepository.findProjectNodeById(projectNodeId);
        if (node == null){
            throw new ApiException("node not found");
        }

        node.setUpdatedAt(LocalDate.now());
        node.setType(projectNode.getType());
        node.setDataJson(projectNode.getDataJson());
        node.setPositionX(projectNode.getPositionX());
        node.setPositionY(projectNode.getPositionY());
        projectNodeRepository.save(node);
    }

    public void deleteNode(Integer nodeId){
        ProjectNode node = projectNodeRepository.findProjectNodeById(nodeId);
        if (node == null){
            throw new ApiException("node not found");
        }
        projectNodeRepository.delete(node);
    }


}
