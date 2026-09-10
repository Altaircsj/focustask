package br.edu.ufersa.pw.focustask.api;

import br.edu.ufersa.pw.focustask.domain.entities.Project;
import br.edu.ufersa.pw.focustask.domain.entities.Task;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    @GetMapping
    public List<Project> getAllProjects() {
        return null;
    }

    @PostMapping
    public Project createProject(@RequestBody Project project) {
        return null;
    }

    @GetMapping("/{projectId}")
    public Project getProjectById(@PathVariable Long projectId) {
        return null;
    }

    @PutMapping("/{projectId}")
    public Project updateProject(
            @PathVariable Long projectId,
            @RequestBody Project project
    ) {
        return null;
    }

    @PatchMapping("/{projectId}")
    public Project partiallyUpdateProject(
            @PathVariable Long projectId,
            @RequestBody Project project
    ) {
        return null;
    }

    @DeleteMapping("/{projectId}")
    public Void deleteProject(@PathVariable Long projectId) {
        return null;
    }

    @GetMapping("/{projectId}/tasks")
    public List<Task> getProjectTasks(
            @PathVariable Long projectId
    ) {
        return null;
    }
}