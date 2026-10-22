package com.example.sana.Controller.CreativityDomainController;

import com.example.sana.API.ApiResponse;
import com.example.sana.DTO.CreativityRelationCreateDTO;
import com.example.sana.Service.CreativityDomainServise.CreativityRelationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/creativity/relations")
@RequiredArgsConstructor
public class CreativityRelationController {

    private final CreativityRelationService creativityRelationService;


    // Get all relationships in a ProjectOutput
    // Used when displaying the complete visual map
    @GetMapping("/get/{outputId}")
    public ResponseEntity<?> getOutputRelations(
            @PathVariable Integer outputId) {

        return ResponseEntity.status(200).body(creativityRelationService.getOutputRelations(outputId));
    }


    // Get all relationships connected to a specific Node
    // Used when the user selects a Character, World, Location, etc.
    // Includes both incoming and outgoing edges
    @GetMapping("/get/{outputId}/node/{nodeId}")
    public ResponseEntity<?> getNodeRelations(
            @PathVariable Integer outputId,
            @PathVariable Integer nodeId) {

        return ResponseEntity.status(200).body(creativityRelationService.getNodeRelations(outputId, nodeId));
    }


    // Get available relationship types between two Nodes
    // Used when the user selects two elements on the canvas
    // All IDs are passed through the Path
    @GetMapping("/get/types/{outputId}/{sourceNodeId}/{targetNodeId}")
    public ResponseEntity<?> getAllowedRelationTypes(
            @PathVariable Integer outputId,
            @PathVariable Integer sourceNodeId,
            @PathVariable Integer targetNodeId) {

        return ResponseEntity.status(200).body(creativityRelationService.getAllowedRelationTypes(outputId, sourceNodeId, targetNodeId));
    }


    // Create a relationship between two creativity Nodes
    // Checks whether the selected relationship type is valid
    // Uses the existing ProjectEdgeService to save the Edge
    @PostMapping("/add/{outputId}/{sourceNodeId}/{targetNodeId}")
    public ResponseEntity<?> addCreativityRelation(
            @PathVariable Integer outputId,
            @PathVariable Integer sourceNodeId,
            @PathVariable Integer targetNodeId,
            @RequestBody @Valid CreativityRelationCreateDTO dto) {

        creativityRelationService.addCreativityRelation(
                outputId, sourceNodeId, targetNodeId, dto);

        return ResponseEntity.status(200).body(new ApiResponse("Creativity relation added successfully"));
    }


    // Preview the impact of deleting a creativity Node
    // Shows affected relationships and possible deletion blockers
    // Does NOT delete the Node or its connected elements
    // outputId = ProjectOutput ID
    // nodeId = ProjectNode ID

    @GetMapping("/delete/preview/{outputId}/node/{nodeId}")
    public ResponseEntity<?> previewNodeDeletion(
            @PathVariable Integer outputId,
            @PathVariable Integer nodeId) {

        return ResponseEntity.status(200).body(creativityRelationService.previewNodeDeletion(outputId, nodeId));
    }




}
