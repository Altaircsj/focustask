package br.edu.ufersa.pw.focustask.api;

import br.edu.ufersa.pw.focustask.domain.entities.Project;
import br.edu.ufersa.pw.focustask.domain.entities.Task;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//TODO com autenticação pode retirar o caminho /users/{userId} de todos aqui

@RestController
@RequestMapping("/api/v1")
public class ProjectController {

    @GetMapping("/users/{userId}/projects")
    public List<Project> getAllProjects(
            @PathVariable Long userId
    ) {
        return null;
    }

    @PostMapping("/users/{userId}/projects")
    public Project createProject(
            @PathVariable Long userId,
            @RequestBody Project project
    ) {
        return null;
    }

    @GetMapping("/users/{userId}/projects/{projectId}")
    public Project getProjectById(
            @PathVariable Long userId,
            @PathVariable Long projectId
    ) {
        return null;
    }

    @PutMapping("/users/{userId}/projects/{projectId}")
    public Project updateProject(
            @PathVariable Long userId,
            @PathVariable Long projectId,
            @RequestBody Project project
    ) {
        return null;
    }

    @PatchMapping("/users/{userId}/projects/{projectId}")
    public Project partiallyUpdateProject(
            @PathVariable Long userId,
            @PathVariable Long projectId,
            @RequestBody Project project
    ) {
        return null;
    }

    @DeleteMapping("/users/{userId}/projects/{projectId}")
    public void deleteProject(
            @PathVariable Long userId,
            @PathVariable Long projectId
    ) {
    }

    @GetMapping("/users/{userId}/projects/{projectId}/tasks")
    public List<Task> getProjectTasks(
            @PathVariable Long userId,
            @PathVariable Long projectId
    ) {
        return null;
    }
}
