package com.example.sana.Service.TechDomainServices;

import com.example.sana.API.ApiException;
import com.example.sana.DTO.TechDomainDTOs.ArchitectureDiagramRequestDTO;
import com.example.sana.DTO.TechDomainDTOs.ArchitectureEdgeRequestDTO;
import com.example.sana.DTO.TechDomainDTOs.ArchitectureNodeRequestDTO;
import com.example.sana.Enums.RepresentationType;
import com.example.sana.Enums.TechDomainEnums.TechnicalOutputType;
import com.example.sana.Model.Project;
import com.example.sana.Model.ProjectEdge;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Repository.ProjectEdgeRepository;
import com.example.sana.Repository.ProjectNodeRepository;
import com.example.sana.Repository.ProjectOutputRepository;
import com.example.sana.Repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ArchitectureDiagramService {

    private final ProjectOutputRepository projectOutputRepository;
    private final ProjectRepository projectRepository;
    private final ProjectNodeRepository projectNodeRepository;
    private final ProjectEdgeRepository projectEdgeRepository;
    private final JsonMapper jsonMapper;

    @Transactional(readOnly = true)
    public List<ProjectOutput> getAllArchitectureDiagrams() {
        return projectOutputRepository.findAll().stream()
                .filter(this::isArchitectureDiagram)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectOutput> getArchitectureDiagramsByProjectId(Integer projectId) {
        if (projectRepository.findProjectById(projectId) == null) {
            throw new ApiException("Project not found");
        }
        return projectOutputRepository.findAllByProject_Id(projectId).stream()
                .filter(this::isArchitectureDiagram)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectOutput getArchitectureDiagramById(Integer outputId) {
        return getArchitectureOutput(outputId);
    }

    @Transactional
    public void addArchitectureDiagram(Integer projectId, ArchitectureDiagramRequestDTO dto) {
        Project project = projectRepository.findProjectById(projectId);
        if (project == null) {
            throw new ApiException("Project not found");
        }

        ProjectOutput output = new ProjectOutput();
        output.setTitle(dto.getTitle());
        output.setOutputType(TechnicalOutputType.ARCHITECTURE.name());
        output.setRepresentationType(RepresentationType.GRAPH);
        output.setProject(project);
        output.setCreatedAt(LocalDateTime.now());
        output.setUpdatedAt(output.getCreatedAt());
        projectOutputRepository.save(output);
    }

    @Transactional
    public void updateArchitectureDiagram(Integer outputId, ArchitectureDiagramRequestDTO dto) {
        ProjectOutput output = getArchitectureOutput(outputId);
        output.setTitle(dto.getTitle());
        output.setUpdatedAt(LocalDateTime.now());
        projectOutputRepository.save(output);
    }

    @Transactional
    public void deleteArchitectureDiagram(Integer outputId) {
        ProjectOutput output = getArchitectureOutput(outputId);
        projectOutputRepository.delete(output);
    }

    @Transactional(readOnly = true)
    public ProjectNode getArchitectureNodeById(Integer outputId, Integer nodeId) {
        getArchitectureOutput(outputId);
        return getOutputNode(outputId, nodeId);
    }

    @Transactional
    public void addArchitectureNode(Integer outputId, ArchitectureNodeRequestDTO dto) {
        ProjectOutput output = getArchitectureOutput(outputId);
        ProjectNode node = new ProjectNode();
        node.setProjectOutput(output);
        node.setCreatedAt(LocalDate.now());
        setNodeFields(node, dto);
        projectNodeRepository.save(node);
    }

    @Transactional
    public void updateArchitectureNode(Integer outputId, Integer nodeId, ArchitectureNodeRequestDTO dto) {
        getArchitectureOutput(outputId);
        ProjectNode node = getOutputNode(outputId, nodeId);
        setNodeFields(node, dto);
        projectNodeRepository.save(node);
    }

    @Transactional
    public void deleteArchitectureNode(Integer outputId, Integer nodeId) {
        getArchitectureOutput(outputId);
        ProjectNode node = getOutputNode(outputId, nodeId);
        projectNodeRepository.delete(node);
    }

    @Transactional(readOnly = true)
    public ProjectEdge getArchitectureEdgeById(Integer outputId, Integer edgeId) {
        getArchitectureOutput(outputId);
        return getOutputEdge(outputId, edgeId);
    }

    @Transactional
    public void addArchitectureEdge(Integer outputId, ArchitectureEdgeRequestDTO dto) {
        ProjectOutput output = getArchitectureOutput(outputId);
        ProjectNode source = getOutputNode(outputId, dto.getSourceNodeId());
        ProjectNode target = getOutputNode(outputId, dto.getTargetNodeId());
        ProjectEdge edge = new ProjectEdge();
        edge.setProjectOutput(output);
        edge.setCreatedAt(LocalDate.now());
        setEdgeFields(edge, dto, source, target);
        projectEdgeRepository.save(edge);
    }

    @Transactional
    public void updateArchitectureEdge(Integer outputId, Integer edgeId, ArchitectureEdgeRequestDTO dto) {
        getArchitectureOutput(outputId);
        ProjectEdge edge = getOutputEdge(outputId, edgeId);
        ProjectNode source = getOutputNode(outputId, dto.getSourceNodeId());
        ProjectNode target = getOutputNode(outputId, dto.getTargetNodeId());
        setEdgeFields(edge, dto, source, target);
        projectEdgeRepository.save(edge);
    }

    @Transactional
    public void deleteArchitectureEdge(Integer outputId, Integer edgeId) {
        getArchitectureOutput(outputId);
        ProjectEdge edge = getOutputEdge(outputId, edgeId);
        projectEdgeRepository.delete(edge);
    }

    private boolean isArchitectureDiagram(ProjectOutput output) {
        return TechnicalOutputType.ARCHITECTURE.name().equals(output.getOutputType())
                && output.getRepresentationType() == RepresentationType.GRAPH;
    }

    private ProjectOutput getArchitectureOutput(Integer outputId) {
        ProjectOutput output = projectOutputRepository.findProjectOutputById(outputId);
        if (output == null) {
            throw new ApiException("Project output not found");
        }
        if (!isArchitectureDiagram(output)) {
            throw new ApiException("Output is not an Architecture Diagram");
        }
        return output;
    }

    private ProjectNode getOutputNode(Integer outputId, Integer nodeId) {
        ProjectNode node = projectNodeRepository.findProjectNodeById(nodeId);
        if (node == null) {
            throw new ApiException("Project node not found");
        }
        if (node.getProjectOutput() == null || !outputId.equals(node.getProjectOutput().getId())) {
            throw new ApiException("Project node does not belong to this Architecture Diagram");
        }
        return node;
    }

    private ProjectEdge getOutputEdge(Integer outputId, Integer edgeId) {
        ProjectEdge edge = projectEdgeRepository.findProjectEdgeById(edgeId);
        if (edge == null) {
            throw new ApiException("Project edge not found");
        }
        if (edge.getProjectOutput() == null || !outputId.equals(edge.getProjectOutput().getId())) {
            throw new ApiException("Project edge does not belong to this Architecture Diagram");
        }
        return edge;
    }

    private void setNodeFields(ProjectNode node, ArchitectureNodeRequestDTO dto) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (dto.getData() != null) {
            data.putAll(dto.getData());
        }
        data.put("label", dto.getLabel());

        node.setType(dto.getType());
        node.setDataJson(jsonMapper.writeValueAsString(data));
        node.setPositionX(dto.getPositionX());
        node.setPositionY(dto.getPositionY());
        node.setUpdatedAt(LocalDate.now());
    }

    private void setEdgeFields(ProjectEdge edge, ArchitectureEdgeRequestDTO dto,
                               ProjectNode source, ProjectNode target) {
        String dataJson = dto.getData() == null ? null : jsonMapper.writeValueAsString(dto.getData());
        edge.setType(dto.getType());
        edge.setLabel(dto.getLabel());
        edge.setDataJson(dataJson);
        edge.setSourceNode(source);
        edge.setTargetNode(target);
    }
}
