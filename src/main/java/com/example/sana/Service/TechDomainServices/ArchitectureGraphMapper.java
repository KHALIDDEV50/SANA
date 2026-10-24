package com.example.sana.Service.TechDomainServices;

import com.example.sana.API.ApiException;
import com.example.sana.DTO.TechDomainDTOs.GraphDefinitionDTO;
import com.example.sana.Enums.RepresentationType;
import com.example.sana.Enums.TechDomainEnums.TechnicalOutputType;
import com.example.sana.Model.ProjectEdge;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Model.ProjectOutput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.DeserializationFeature;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ArchitectureGraphMapper {

    private final JsonMapper jsonMapper;

    // The caller loads the graph inside its read transaction. This mapper performs no writes.
    public GraphDefinitionDTO toDTO(ProjectOutput output) {
        if (output == null || !TechnicalOutputType.ARCHITECTURE.name().equals(output.getOutputType())
                || output.getRepresentationType() != RepresentationType.GRAPH) {
            throw new ApiException("Output must be an Architecture Diagram");
        }
        return toGraphDTO(output);
    }

    // Reuse the storage mapping for advice-only domains without claiming their graph rules are implemented.
    public GraphDefinitionDTO toGraphDTO(ProjectOutput output) {
        requireId(output.getId(), "Architecture diagram");
        if (output.getTitle() == null || output.getTitle().isBlank() || output.getTitle().length() > 255) {
            throw new ApiException("Architecture diagram title is invalid");
        }
        if (output.getNodes() == null || output.getEdges() == null) {
            throw new ApiException("Architecture graph collections must be loaded");
        }

        var nodes = new ArrayList<GraphDefinitionDTO.NodeDTO>();
        Set<Integer> nodeIds = new HashSet<>();
        for (ProjectNode node : output.getNodes()) {
            if (node == null) throw new ApiException("Architecture graph contains a missing node");
            requireId(node.getId(), "Node");
            requireOutput(node.getProjectOutput(), output.getId());
            requireText(node.getType(), "Node type", 50);
            if (!nodeIds.add(node.getId())) throw new ApiException("Architecture graph contains duplicate node IDs");
            if (node.getPositionX() == null || node.getPositionY() == null
                    || !Double.isFinite(node.getPositionX()) || !Double.isFinite(node.getPositionY())) {
                throw new ApiException("Node positions must be finite numbers");
            }
            nodes.add(new GraphDefinitionDTO.NodeDTO("node:" + node.getId(), node.getId(), node.getType(),
                    readProperties(node.getDataJson()), node.getPositionX(), node.getPositionY()));
        }

        var edges = new ArrayList<GraphDefinitionDTO.EdgeDTO>();
        Set<Integer> edgeIds = new HashSet<>();
        for (ProjectEdge edge : output.getEdges()) {
            if (edge == null) throw new ApiException("Architecture graph contains a missing edge");
            requireId(edge.getId(), "Edge");
            requireOutput(edge.getProjectOutput(), output.getId());
            requireText(edge.getType(), "Edge type", 50);
            requireText(edge.getLabel(), "Edge label", 50);
            if (!edgeIds.add(edge.getId())) throw new ApiException("Architecture graph contains duplicate edge IDs");
            requireEndpoint(edge.getSourceNode(), nodeIds, output.getId());
            requireEndpoint(edge.getTargetNode(), nodeIds, output.getId());
            edges.add(new GraphDefinitionDTO.EdgeDTO("edge:" + edge.getId(), edge.getId(), edge.getType(),
                    edge.getLabel(), "node:" + edge.getSourceNode().getId(), "node:" + edge.getTargetNode().getId(),
                    readProperties(edge.getDataJson())));
        }

        // Stable ordering keeps snapshots and later computed differences predictable.
        nodes.sort(Comparator.comparing(GraphDefinitionDTO.NodeDTO::getId));
        edges.sort(Comparator.comparing(GraphDefinitionDTO.EdgeDTO::getId));
        Map<String, Object> properties = readProperties(output.getPropertiesJson());
        properties.put("title", output.getTitle());
        return new GraphDefinitionDTO(nodes, edges, properties);
    }

    private void requireEndpoint(ProjectNode node, Set<Integer> nodeIds, Integer outputId) {
        if (node == null || !nodeIds.contains(node.getId())) {
            throw new ApiException("Edge endpoint must reference a node in the Architecture Diagram");
        }
        requireOutput(node.getProjectOutput(), outputId);
    }

    private void requireOutput(ProjectOutput actual, Integer expectedId) {
        if (actual == null || !expectedId.equals(actual.getId())) {
            throw new ApiException("Graph element belongs to another output");
        }
    }

    private void requireId(Integer id, String element) {
        if (id == null || id <= 0) throw new ApiException(element + " must have a saved positive ID");
    }

    private void requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new ApiException(field + " is invalid");
        }
    }

    private Map<String, Object> readProperties(String dataJson) {
        // Nullable database metadata is represented as an empty JSON object.
        if (dataJson == null) return new LinkedHashMap<>();
        try {
            Map<String, Object> data = jsonMapper.readerFor(Map.class)
                    .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readValue(dataJson);
            if (data == null) {
                throw new ApiException("Graph properties must be a JSON object");
            }
            return data;
        } catch (JacksonException e) {
            throw new ApiException("Graph properties contain invalid JSON");
        }
    }
}
