package com.example.sana.Service.TechDomainServices;

import com.example.sana.API.ApiException;
import com.example.sana.API.ApiConflictException;
import com.example.sana.DTO.TechDomainDTOs.ArchitectureGraphResponseDTO;
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
import java.util.HashSet;
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
    private final ArchitectureGraphMapper architectureGraphMapper;

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

        if (!"TECH".equals(project.getDomain())) throw new ApiException("Architecture diagrams require a TECH project");
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
    public void updateArchitectureDiagram(Integer outputId, ArchitectureDiagramRequestDTO dto, Integer expectedRevision) {
        ProjectOutput output = lockArchitectureOutput(outputId, expectedRevision);
        output.setTitle(dto.getTitle());
        advanceRevision(output);
    }

    @Transactional
    public void deleteArchitectureDiagram(Integer outputId, Integer expectedRevision) {
        ProjectOutput output = lockArchitectureOutput(outputId, expectedRevision);
        projectOutputRepository.delete(output);
    }

    @Transactional(readOnly = true)
    public ProjectNode getArchitectureNodeById(Integer outputId, Integer nodeId) {
        getArchitectureOutput(outputId);
        return getOutputNode(outputId, nodeId);
    }

    @Transactional
    public void addArchitectureNode(Integer outputId, ArchitectureNodeRequestDTO dto, Integer expectedRevision) {
        ProjectOutput output = lockArchitectureOutput(outputId, expectedRevision);
        ProjectNode node = new ProjectNode();
        node.setProjectOutput(output);
        node.setCreatedAt(LocalDate.now());
        setNodeFields(node, dto);
        projectNodeRepository.save(node);
        output.getNodes().add(node);
        advanceRevision(output);
    }

    @Transactional
    public void updateArchitectureNode(Integer outputId, Integer nodeId, ArchitectureNodeRequestDTO dto, Integer expectedRevision) {
        ProjectOutput output = lockArchitectureOutput(outputId, expectedRevision);
        ProjectNode node = getOutputNode(outputId, nodeId);
        setNodeFields(node, dto);
        var connected = new HashSet<ProjectEdge>();
        if (node.getIncomingEdges() != null) connected.addAll(node.getIncomingEdges());
        if (node.getOutgoingEdges() != null) connected.addAll(node.getOutgoingEdges());
        for (var edge : connected) ArchitectureGraphRules.edge(edge.getType(), edge.getLabel(),
                edge.getSourceNode().getType(), edge.getTargetNode().getType(), Map.of());
        projectNodeRepository.save(node);
        advanceRevision(output);
    }

    @Transactional
    public void deleteArchitectureNode(Integer outputId, Integer nodeId, Integer expectedRevision) {
        ProjectOutput output = lockArchitectureOutput(outputId, expectedRevision);
        ProjectNode node = getOutputNode(outputId, nodeId);
        // Remove incident edges and both sides of the in-memory graph before deleting.
        // Otherwise cascading parent/endpoint collections can retain deleted entities.
        var incidentEdges = new HashSet<ProjectEdge>();
        if (node.getOutgoingEdges() != null) incidentEdges.addAll(node.getOutgoingEdges());
        if (node.getIncomingEdges() != null) incidentEdges.addAll(node.getIncomingEdges());
        for (ProjectEdge edge : incidentEdges) {
            detachEdge(output, edge);
            projectEdgeRepository.delete(edge);
        }
        output.getNodes().remove(node);
        projectNodeRepository.delete(node);
        advanceRevision(output);
    }

    @Transactional(readOnly = true)
    public ProjectEdge getArchitectureEdgeById(Integer outputId, Integer edgeId) {
        getArchitectureOutput(outputId);
        return getOutputEdge(outputId, edgeId);
    }

    @Transactional
    public void addArchitectureEdge(Integer outputId, ArchitectureEdgeRequestDTO dto, Integer expectedRevision) {
        ProjectOutput output = lockArchitectureOutput(outputId, expectedRevision);
        ProjectNode source = getOutputNode(outputId, dto.getSourceNodeId());
        ProjectNode target = getOutputNode(outputId, dto.getTargetNodeId());
        ProjectEdge edge = new ProjectEdge();
        edge.setProjectOutput(output);
        edge.setCreatedAt(LocalDate.now());
        setEdgeFields(edge, dto, source, target);
        projectEdgeRepository.save(edge);
        output.getEdges().add(edge);
        advanceRevision(output);
    }

    @Transactional
    public void updateArchitectureEdge(Integer outputId, Integer edgeId, ArchitectureEdgeRequestDTO dto, Integer expectedRevision) {
        ProjectOutput output = lockArchitectureOutput(outputId, expectedRevision);
        ProjectEdge edge = getOutputEdge(outputId, edgeId);
        ProjectNode source = getOutputNode(outputId, dto.getSourceNodeId());
        ProjectNode target = getOutputNode(outputId, dto.getTargetNodeId());
        setEdgeFields(edge, dto, source, target);
        projectEdgeRepository.save(edge);
        advanceRevision(output);
    }

    @Transactional
    public void deleteArchitectureEdge(Integer outputId, Integer edgeId, Integer expectedRevision) {
        ProjectOutput output = lockArchitectureOutput(outputId, expectedRevision);
        ProjectEdge edge = getOutputEdge(outputId, edgeId);
        detachEdge(output, edge);
        projectEdgeRepository.delete(edge);
        advanceRevision(output);
    }

    // Use the same parent lock as writers so graph and revision describe one snapshot.
    @Transactional
    public ArchitectureGraphResponseDTO getArchitectureGraph(Integer outputId) {
        ProjectOutput output = lockArchitectureOutput(outputId);
        return new ArchitectureGraphResponseDTO(outputId, output.getGraphRevision(), architectureGraphMapper.toDTO(output));
    }

    private ProjectOutput lockArchitectureOutput(Integer outputId) {
        ProjectOutput output = projectOutputRepository.findByIdForUpdate(outputId)
                .orElseThrow(() -> new ApiException("Project output not found"));
        if (!isArchitectureDiagram(output)) throw new ApiException("Output is not an Architecture Diagram");
        if (output.getGraphRevision() == null) throw new ApiException("Graph revision migration is required");
        return output;
    }

    private ProjectOutput lockArchitectureOutput(Integer outputId, Integer expectedRevision) {
        if (expectedRevision == null || expectedRevision < 0) {
            throw new ApiException("Expected revision must be a non-negative integer");
        }
        ProjectOutput output = lockArchitectureOutput(outputId);
        if (!expectedRevision.equals(output.getGraphRevision())) {
            throw new ApiConflictException("Architecture diagram changed. Reload the graph and retry with its current revision");
        }
        if (output.getGraphRevision() == Integer.MAX_VALUE) throw new ApiConflictException("Graph revision limit reached");
        return output;
    }

    private void advanceRevision(ProjectOutput output) {
        output.setGraphRevision(output.getGraphRevision() + 1);
        output.setUpdatedAt(LocalDateTime.now());
        // The locked output is managed; dirty checking commits its revision atomically.
    }

    private void detachEdge(ProjectOutput output, ProjectEdge edge) {
        output.getEdges().remove(edge);
        if (edge.getSourceNode() != null && edge.getSourceNode().getOutgoingEdges() != null) {
            edge.getSourceNode().getOutgoingEdges().remove(edge);
        }
        if (edge.getTargetNode() != null && edge.getTargetNode().getIncomingEdges() != null) {
            edge.getTargetNode().getIncomingEdges().remove(edge);
        }
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

        ArchitectureGraphRules.node(dto.getType(), data, dto.getPositionX(), dto.getPositionY());
        node.setType(dto.getType());
        node.setDataJson(jsonMapper.writeValueAsString(data));
        node.setPositionX(dto.getPositionX());
        node.setPositionY(dto.getPositionY());
        node.setUpdatedAt(LocalDate.now());
    }

    private void setEdgeFields(ProjectEdge edge, ArchitectureEdgeRequestDTO dto,
                               ProjectNode source, ProjectNode target) {
        Map<String, Object> properties = dto.getData() == null ? Map.of() : dto.getData();
        if (source.getId().equals(target.getId())) throw new ApiException("Architecture edges require distinct endpoints");
        ArchitectureGraphRules.edge(dto.getType(), dto.getLabel(), source.getType(), target.getType(), properties);
        String dataJson = dto.getData() == null ? null : jsonMapper.writeValueAsString(dto.getData());
        edge.setType(dto.getType());
        edge.setLabel(dto.getLabel());
        edge.setDataJson(dataJson);
        edge.setSourceNode(source);
        edge.setTargetNode(target);
    }
}
