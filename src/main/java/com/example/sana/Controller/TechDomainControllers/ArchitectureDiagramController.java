package com.example.sana.Controller.TechDomainControllers;

import com.example.sana.API.ApiResponse;
import com.example.sana.DTO.DiagramDTOs.DiagramEdgeRequestDTO;
import com.example.sana.DTO.DiagramDTOs.DiagramGraphResponseDTO;
import com.example.sana.DTO.DiagramDTOs.DiagramNodeRequestDTO;
import com.example.sana.DTO.DiagramDTOs.DiagramRequestDTO;
import com.example.sana.Enums.TechDomainEnums.TechnicalOutputType;
import com.example.sana.Model.ProjectEdge;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Service.DiagramServices.DiagramService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Architecture routes kept for existing clients. They use the shared DiagramService limited to ARCHITECTURE.
@RestController
@RequestMapping("/api/v1/tech/architecture")
@RequiredArgsConstructor
public class ArchitectureDiagramController {

    private static final String ARCHITECTURE = TechnicalOutputType.ARCHITECTURE.name();

    private final DiagramService diagramService;

    @GetMapping("/graph/get/{outputId}")
    public ResponseEntity<DiagramGraphResponseDTO> getArchitectureGraph(@PathVariable Integer outputId) {
        return ResponseEntity.status(200).body(diagramService.getGraph(outputId, ARCHITECTURE));
    }

    @GetMapping("/get")
    public ResponseEntity<List<ProjectOutput>> getAllArchitectureDiagrams() {
        return ResponseEntity.status(200).body(diagramService.getDiagramsByType(ARCHITECTURE));
    }

    @GetMapping("/get/{outputId}")
    public ResponseEntity<ProjectOutput> getArchitectureDiagramById(@PathVariable Integer outputId) {
        return ResponseEntity.status(200).body(diagramService.getDiagram(outputId, ARCHITECTURE));
    }

    @GetMapping("/get/project/{projectId}")
    public ResponseEntity<List<ProjectOutput>> getArchitectureDiagramsByProjectId(@PathVariable Integer projectId) {
        return ResponseEntity.status(200).body(diagramService.getProjectDiagrams(projectId, ARCHITECTURE));
    }

    @PostMapping("/add/{projectId}")
    public ResponseEntity<ApiResponse> addArchitectureDiagram(@PathVariable Integer projectId,
                                                              @Valid @RequestBody DiagramRequestDTO dto) {
        dto.setDiagramType(ARCHITECTURE);
        diagramService.addDiagram(projectId, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Architecture diagram added successfully"));
    }

    @PutMapping("/update/{outputId}")
    public ResponseEntity<ApiResponse> updateArchitectureDiagram(@PathVariable Integer outputId,
                                                                 @Valid @RequestBody DiagramRequestDTO dto, @RequestParam Integer expectedRevision) {
        diagramService.updateDiagram(outputId, dto, expectedRevision, ARCHITECTURE);
        return ResponseEntity.status(200).body(new ApiResponse("Architecture diagram updated successfully"));
    }

    @DeleteMapping("/delete/{outputId}")
    public ResponseEntity<ApiResponse> deleteArchitectureDiagram(@PathVariable Integer outputId, @RequestParam Integer expectedRevision) {
        diagramService.deleteDiagram(outputId, expectedRevision, ARCHITECTURE);
        return ResponseEntity.status(200).body(new ApiResponse("Architecture diagram deleted successfully"));
    }

    @GetMapping("/node/get/{outputId}/{nodeId}")
    public ResponseEntity<ProjectNode> getArchitectureNodeById(@PathVariable Integer outputId, @PathVariable Integer nodeId) {
        return ResponseEntity.status(200).body(diagramService.getNode(outputId, nodeId, ARCHITECTURE));
    }

    @PostMapping("/node/add/{outputId}")
    public ResponseEntity<ApiResponse> addArchitectureNode(@PathVariable Integer outputId,
                                                           @Valid @RequestBody DiagramNodeRequestDTO dto, @RequestParam Integer expectedRevision) {
        diagramService.addNode(outputId, dto, expectedRevision, ARCHITECTURE);
        return ResponseEntity.status(200).body(new ApiResponse("Architecture node added successfully"));
    }

    @PutMapping("/node/update/{outputId}/{nodeId}")
    public ResponseEntity<ApiResponse> updateArchitectureNode(@PathVariable Integer outputId, @PathVariable Integer nodeId,
                                                              @Valid @RequestBody DiagramNodeRequestDTO dto, @RequestParam Integer expectedRevision) {
        diagramService.updateNode(outputId, nodeId, dto, expectedRevision, ARCHITECTURE);
        return ResponseEntity.status(200).body(new ApiResponse("Architecture node updated successfully"));
    }

    @DeleteMapping("/node/delete/{outputId}/{nodeId}")
    public ResponseEntity<ApiResponse> deleteArchitectureNode(@PathVariable Integer outputId, @PathVariable Integer nodeId, @RequestParam Integer expectedRevision) {
        diagramService.deleteNode(outputId, nodeId, expectedRevision, ARCHITECTURE);
        return ResponseEntity.status(200).body(new ApiResponse("Architecture node deleted successfully"));
    }

    @GetMapping("/edge/get/{outputId}/{edgeId}")
    public ResponseEntity<ProjectEdge> getArchitectureEdgeById(@PathVariable Integer outputId, @PathVariable Integer edgeId) {
        return ResponseEntity.status(200).body(diagramService.getEdge(outputId, edgeId, ARCHITECTURE));
    }

    @PostMapping("/edge/add/{outputId}")
    public ResponseEntity<ApiResponse> addArchitectureEdge(@PathVariable Integer outputId,
                                                           @Valid @RequestBody DiagramEdgeRequestDTO dto, @RequestParam Integer expectedRevision) {
        diagramService.addEdge(outputId, dto, expectedRevision, ARCHITECTURE);
        return ResponseEntity.status(200).body(new ApiResponse("Architecture edge added successfully"));
    }

    @PutMapping("/edge/update/{outputId}/{edgeId}")
    public ResponseEntity<ApiResponse> updateArchitectureEdge(@PathVariable Integer outputId, @PathVariable Integer edgeId,
                                                              @Valid @RequestBody DiagramEdgeRequestDTO dto, @RequestParam Integer expectedRevision) {
        diagramService.updateEdge(outputId, edgeId, dto, expectedRevision, ARCHITECTURE);
        return ResponseEntity.status(200).body(new ApiResponse("Architecture edge updated successfully"));
    }

    @DeleteMapping("/edge/delete/{outputId}/{edgeId}")
    public ResponseEntity<ApiResponse> deleteArchitectureEdge(@PathVariable Integer outputId, @PathVariable Integer edgeId, @RequestParam Integer expectedRevision) {
        diagramService.deleteEdge(outputId, edgeId, expectedRevision, ARCHITECTURE);
        return ResponseEntity.status(200).body(new ApiResponse("Architecture edge deleted successfully"));
    }
}
