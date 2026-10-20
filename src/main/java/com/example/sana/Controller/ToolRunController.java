package com.example.sana.Controller;

import com.example.sana.API.ApiResponse;
import com.example.sana.Model.ToolRun;
import com.example.sana.Service.ToolRunService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tool-runs")
@RequiredArgsConstructor
public class ToolRunController {

    private final ToolRunService toolRunService;

    @GetMapping
    public ResponseEntity<?> getAllToolRuns() {
        return ResponseEntity.status(200).body(toolRunService.getAllToolRuns());
    }

    @GetMapping("/{tool_run_id}")
    public ResponseEntity<?> getToolRunById(@PathVariable Integer tool_run_id) {
        return ResponseEntity.status(200).body(toolRunService.getToolRunById(tool_run_id));
    }

    @PostMapping
    public ResponseEntity<?> addToolRun(@RequestParam Long user_id, @RequestParam Long project_id,
                                       @Valid @RequestBody ToolRun toolRun) {
        toolRunService.addToolRun(user_id, project_id, toolRun);
        return ResponseEntity.status(200).body(new ApiResponse("Tool run added successfully"));
    }

    @PutMapping("/{tool_run_id}")
    public ResponseEntity<?> updateToolRun(@PathVariable Integer tool_run_id, @Valid @RequestBody ToolRun toolRun) {
        toolRunService.updateToolRun(tool_run_id, toolRun);
        return ResponseEntity.status(200).body(new ApiResponse("Tool run updated successfully"));
    }

    @DeleteMapping("/{tool_run_id}")
    public ResponseEntity<?> deleteToolRun(@PathVariable Integer tool_run_id) {
        toolRunService.deleteToolRun(tool_run_id);
        return ResponseEntity.status(200).body(new ApiResponse("Tool run deleted successfully"));
    }
}
