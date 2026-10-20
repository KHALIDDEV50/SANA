package com.example.sana.Service;

import com.example.sana.API.ApiException;
import com.example.sana.Model.Project;
import com.example.sana.Model.ToolRun;
import com.example.sana.Model.User;
import com.example.sana.Repository.ProjectRepository;
import com.example.sana.Repository.ToolRunRepository;
import com.example.sana.Repository.AuthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ToolRunService {

    private final ToolRunRepository toolRunRepository;
    private final AuthRepository authRepository;
    private final ProjectRepository projectRepository;

    public List<ToolRun> getAllToolRuns() {
        return toolRunRepository.findAll();
    }

    public ToolRun getToolRunById(Integer tool_run_id) {
        return toolRunRepository.findById(tool_run_id)
                .orElseThrow(() -> new ApiException("Tool run not found"));
    }

    public void addToolRun(Long user_id, Long project_id, ToolRun toolRun) {
        User user = authRepository.findById(user_id)
                .orElseThrow(() -> new ApiException("User not found"));
        Project project = projectRepository.findById(project_id)
                .orElseThrow(() -> new ApiException("Project not found"));

        toolRun.setId(null);
        toolRun.setCreatedAt(null);
        toolRun.setUser(user);
        toolRun.setProject(project);
        toolRunRepository.save(toolRun);
    }

    public void updateToolRun(Integer tool_run_id, ToolRun toolRun) {
        ToolRun oldToolRun = toolRunRepository.findById(tool_run_id)
                .orElseThrow(() -> new ApiException("Tool run not found"));

        oldToolRun.setToolType(toolRun.getToolType());
        oldToolRun.setInputJson(toolRun.getInputJson());
        oldToolRun.setOutputJson(toolRun.getOutputJson());
        oldToolRun.setStatus(toolRun.getStatus());

        toolRunRepository.save(oldToolRun);
    }

    public void deleteToolRun(Integer tool_run_id) {
        ToolRun toolRun = toolRunRepository.findById(tool_run_id)
                .orElseThrow(() -> new ApiException("Tool run not found"));

        toolRunRepository.delete(toolRun);
    }
}
