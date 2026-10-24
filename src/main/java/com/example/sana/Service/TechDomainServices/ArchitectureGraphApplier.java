package com.example.sana.Service.TechDomainServices;

import com.example.sana.API.ApiConflictException;
import com.example.sana.DTO.TechDomainDTOs.GraphDefinitionDTO;
import com.example.sana.Model.*;
import com.example.sana.Repository.*;
import com.example.sana.Service.IntegrationServices.AIJson;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArchitectureGraphApplier {
    private final ProjectNodeRepository nodes;
    private final ProjectEdgeRepository edges;
    private final AIJson json;

    // Caller owns the output lock and transaction and has validated this entire graph.
    public Map<String, Integer> apply(ProjectOutput output, GraphDefinitionDTO graph) {
        Map<Integer, ProjectNode> oldNodes = output.getNodes().stream().collect(Collectors.toMap(ProjectNode::getId, node -> node));
        Map<Integer, ProjectEdge> oldEdges = output.getEdges().stream().collect(Collectors.toMap(ProjectEdge::getId, edge -> edge));
        Set<Integer> retainedEdges = graph.getEdges().stream().map(GraphDefinitionDTO.EdgeDTO::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        for (ProjectEdge edge : new ArrayList<>(output.getEdges())) if (!retainedEdges.contains(edge.getId())) {
            detach(output, edge); edges.delete(edge);
        }
        Map<String, ProjectNode> byKey = new HashMap<>(); Map<String, Integer> ids = new LinkedHashMap<>();
        Set<Integer> retainedNodes = new HashSet<>();
        for (var dto : graph.getNodes()) {
            ProjectNode node = dto.getId() == null ? new ProjectNode() : oldNodes.get(dto.getId());
            if (node == null) throw new ApiConflictException("A retained node is no longer present");
            if (node.getId() == null) {node.setCreatedAt(LocalDate.now()); node.setProjectOutput(output);}
            node.setType(dto.getType()); node.setDataJson(json.write(dto.getProperties()));
            node.setPositionX(dto.getPositionX()); node.setPositionY(dto.getPositionY()); node.setUpdatedAt(LocalDate.now());
            if (node.getOutgoingEdges() == null) node.setOutgoingEdges(new HashSet<>());
            if (node.getIncomingEdges() == null) node.setIncomingEdges(new HashSet<>());
            nodes.save(node); output.getNodes().add(node); retainedNodes.add(node.getId());
            byKey.put(dto.getKey(), node); ids.put(dto.getKey(), node.getId());
        }
        for (var dto : graph.getEdges()) {
            ProjectEdge edge = dto.getId() == null ? new ProjectEdge() : oldEdges.get(dto.getId());
            if (edge == null) throw new ApiConflictException("A retained edge is no longer present");
            if (edge.getId() == null) {edge.setCreatedAt(LocalDate.now()); edge.setProjectOutput(output);}
            else detach(output, edge);
            edge.setSourceNode(byKey.get(dto.getSourceKey())); edge.setTargetNode(byKey.get(dto.getTargetKey()));
            edge.setType(dto.getType()); edge.setLabel(dto.getLabel()); edge.setDataJson(json.write(dto.getProperties()));
            edges.save(edge); output.getEdges().add(edge);
            edge.getSourceNode().getOutgoingEdges().add(edge); edge.getTargetNode().getIncomingEdges().add(edge);
            ids.put(dto.getKey(), edge.getId());
        }
        for (ProjectNode node : new ArrayList<>(output.getNodes())) if (!retainedNodes.contains(node.getId())) {
            if (node.getCharacterProfile() != null || node.getWorld() != null || node.getLocation() != null || node.getWorldRule() != null || node.getStoryEvent() != null)
                throw new ApiConflictException("Architecture contains domain-specific data requiring manual cleanup");
            output.getNodes().remove(node); nodes.delete(node);
        }
        Map<String, Object> properties = new LinkedHashMap<>(graph.getProperties());
        output.setTitle((String) properties.remove("title")); output.setPropertiesJson(json.write(properties));
        if (output.getGraphRevision() == Integer.MAX_VALUE) throw new ApiConflictException("Graph revision limit reached");
        output.setGraphRevision(output.getGraphRevision() + 1); output.setUpdatedAt(LocalDateTime.now());
        return ids;
    }

    private void detach(ProjectOutput output, ProjectEdge edge) {
        output.getEdges().remove(edge);
        if (edge.getSourceNode().getOutgoingEdges() != null) edge.getSourceNode().getOutgoingEdges().remove(edge);
        if (edge.getTargetNode().getIncomingEdges() != null) edge.getTargetNode().getIncomingEdges().remove(edge);
    }
}
