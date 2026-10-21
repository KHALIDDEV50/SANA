package com.example.sana.Service;

import com.example.sana.API.ApiException;
import com.example.sana.Model.Project;
import com.example.sana.Model.ProjectEdge;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Repository.ProjectEdgeRepository;
import com.example.sana.Repository.ProjectNodeRepository;
import com.example.sana.Repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectEdgeService {

    private final ProjectEdgeRepository projectEdgeRepository;
    private final ProjectRepository projectRepository;
    private final ProjectNodeRepository projectNodeRepository;

    public List<ProjectEdge> getAllEdges(){
        return projectEdgeRepository.findAll();
    }

    public void addEdge(Integer projectId, Integer sourceNodeId, Integer targetNodeId, ProjectEdge projectEdge){
        Project project = projectRepository.findProjectById(projectId);
        if (project == null){
            throw new ApiException("project not found");
        }

        ProjectNode sourceNode = projectNodeRepository.findProjectNodeById(sourceNodeId);
        ProjectNode targetNode = projectNodeRepository.findProjectNodeById(targetNodeId);

        if(sourceNode == null || targetNode == null){
            throw new ApiException("source node or target node not found");
        }

        projectEdge.setProject(project);
        projectEdge.setSourceNode(sourceNode);
        projectEdge.setTargetNode(targetNode);
        projectEdge.setCreatedAt(LocalDate.now());
        projectEdgeRepository.save(projectEdge);
    }

    public void updateEdge(Integer projectEdgeId, ProjectEdge projectEdge){
        ProjectEdge edge = projectEdgeRepository.findProjectEdgeById(projectEdgeId);
        if (edge == null){
            throw new ApiException("project edge not found");
        }

        edge.setDataJson(projectEdge.getDataJson());
        edge.setType(projectEdge.getType());
        edge.setLabel(projectEdge.getLabel());
        projectEdgeRepository.save(edge);
    }

    public void deleteEdge(Integer edgeId){
        ProjectEdge edge = projectEdgeRepository.findProjectEdgeById(edgeId);
        if (edge == null){
            throw new ApiException("project edge not found");
        }
        projectEdgeRepository.delete(edge);
    }


}
