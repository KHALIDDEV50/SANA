package com.example.sana.Controller.TechDomainControllers;

import com.example.sana.API.ApiResponse;
import com.example.sana.DTO.TechDomainDTOs.ArchitectureDiagramRequestDTO;
import com.example.sana.DTO.TechDomainDTOs.ArchitectureEdgeRequestDTO;
import com.example.sana.DTO.TechDomainDTOs.ArchitectureNodeRequestDTO;
import com.example.sana.Model.ProjectEdge;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Service.TechDomainServices.ArchitectureDiagramService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tech/architecture")
@RequiredArgsConstructor
public class ArchitectureDiagramController {

    private final ArchitectureDiagramService architectureDiagramService;

    @GetMapping("/get")
    public ResponseEntity<List<ProjectOutput>> getAllArchitectureDiagrams() {
        return ResponseEntity.status(200).body(architectureDiagramService.getAllArchitectureDiagrams());
    }

    @GetMapping("/get/{outputId}")
    public ResponseEntity<ProjectOutput> getArchitectureDiagramById(@PathVariable Integer outputId) {
        return ResponseEntity.status(200).body(architectureDiagramService.getArchitectureDiagramById(outputId));
    }

    @GetMapping("/get/project/{projectId}")
    public ResponseEntity<List<ProjectOutput>> getArchitectureDiagramsByProjectId(@PathVariable Integer projectId) {
        return ResponseEntity.status(200).body(architectureDiagramService.getArchitectureDiagramsByProjectId(projectId));
    }

    @PostMapping("/add/{projectId}")
    public ResponseEntity<ApiResponse> addArchitectureDiagram(@PathVariable Integer projectId,
                                                            @Valid @RequestBody ArchitectureDiagramRequestDTO dto) {
        architectureDiagramService.addArchitectureDiagram(projectId, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Architecture diagram added successfully"));
    }

    @PutMapping("/update/{outputId}")
    public ResponseEntity<ApiResponse> updateArchitectureDiagram(@PathVariable Integer outputId,
                                                               @Valid @RequestBody ArchitectureDiagramRequestDTO dto) {
        architectureDiagramService.updateArchitectureDiagram(outputId, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Architecture diagram updated successfully"));
    }

    @DeleteMapping("/delete/{outputId}")
    public ResponseEntity<ApiResponse> deleteArchitectureDiagram(@PathVariable Integer outputId) {
        architectureDiagramService.deleteArchitectureDiagram(outputId);
        return ResponseEntity.status(200).body(new ApiResponse("Architecture diagram deleted successfully"));
    }

    @GetMapping("/node/get/{outputId}/{nodeId}")
    public ResponseEntity<ProjectNode> getArchitectureNodeById(@PathVariable Integer outputId, @PathVariable Integer nodeId) {
        return ResponseEntity.status(200).body(architectureDiagramService.getArchitectureNodeById(outputId, nodeId));
    }

    @PostMapping("/node/add/{outputId}")
    public ResponseEntity<ApiResponse> addArchitectureNode(@PathVariable Integer outputId,
                                                         @Valid @RequestBody ArchitectureNodeRequestDTO dto) {
        architectureDiagramService.addArchitectureNode(outputId, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Architecture node added successfully"));
    }

    @PutMapping("/node/update/{outputId}/{nodeId}")
    public ResponseEntity<ApiResponse> updateArchitectureNode(@PathVariable Integer outputId, @PathVariable Integer nodeId,
                                                            @Valid @RequestBody ArchitectureNodeRequestDTO dto) {
        architectureDiagramService.updateArchitectureNode(outputId, nodeId, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Architecture node updated successfully"));
    }

    @DeleteMapping("/node/delete/{outputId}/{nodeId}")
    public ResponseEntity<ApiResponse> deleteArchitectureNode(@PathVariable Integer outputId, @PathVariable Integer nodeId) {
        architectureDiagramService.deleteArchitectureNode(outputId, nodeId);
        return ResponseEntity.status(200).body(new ApiResponse("Architecture node deleted successfully"));
    }

    @GetMapping("/edge/get/{outputId}/{edgeId}")
    public ResponseEntity<ProjectEdge> getArchitectureEdgeById(@PathVariable Integer outputId, @PathVariable Integer edgeId) {
        return ResponseEntity.status(200).body(architectureDiagramService.getArchitectureEdgeById(outputId, edgeId));
    }

    @PostMapping("/edge/add/{outputId}")
    public ResponseEntity<ApiResponse> addArchitectureEdge(@PathVariable Integer outputId,
                                                         @Valid @RequestBody ArchitectureEdgeRequestDTO dto) {
        architectureDiagramService.addArchitectureEdge(outputId, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Architecture edge added successfully"));
    }

    @PutMapping("/edge/update/{outputId}/{edgeId}")
    public ResponseEntity<ApiResponse> updateArchitectureEdge(@PathVariable Integer outputId, @PathVariable Integer edgeId,
                                                            @Valid @RequestBody ArchitectureEdgeRequestDTO dto) {
        architectureDiagramService.updateArchitectureEdge(outputId, edgeId, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Architecture edge updated successfully"));
    }

    @DeleteMapping("/edge/delete/{outputId}/{edgeId}")
    public ResponseEntity<ApiResponse> deleteArchitectureEdge(@PathVariable Integer outputId, @PathVariable Integer edgeId) {
        architectureDiagramService.deleteArchitectureEdge(outputId, edgeId);
        return ResponseEntity.status(200).body(new ApiResponse("Architecture edge deleted successfully"));
    }
}
