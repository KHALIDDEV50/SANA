package com.example.sana.Controller.CreativityDomainController;

import com.example.sana.Service.CreativityDomainServices.YarnExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/branching")
@RequiredArgsConstructor
public class YarnExportController {

    private final YarnExportService yarnExportService;

    // Export a branching narrative using the first scene as the start.
    @GetMapping("/export/yarn/{outputId}")
    public ResponseEntity<?> exportYarn(@PathVariable Integer outputId) {
        return downloadYarn(outputId, null);
    }

    // Export a branching narrative with a selected starting scene.
    // startNodeId refers to BranchingNode.id.
    @GetMapping("/export/yarn/{outputId}/start/{startNodeId}")
    public ResponseEntity<?> exportYarnWithStart(@PathVariable Integer outputId, @PathVariable Integer startNodeId) {
        return downloadYarn(outputId, startNodeId);
    }

    // Return a downloadable .yarn file without saving it to the database.
    private ResponseEntity<?> downloadYarn(Integer outputId, Integer startNodeId) {
        String content = yarnExportService.exportYarn(outputId, startNodeId);

        return ResponseEntity.status(200).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"SANA_" + outputId + ".yarn\"").contentType(MediaType.parseMediaType("text/plain;charset=UTF-8")).body(content);
    }
}
