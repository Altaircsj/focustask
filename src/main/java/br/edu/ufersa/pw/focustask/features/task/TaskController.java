package br.edu.ufersa.pw.focustask.features.task;

import br.edu.ufersa.pw.focustask.features.project.ProjectInternalApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

//TODO com autenticação pode retirar o caminho /users/{userId} de todos aqui

@RestController
@RequestMapping("/api/v1")
public class TaskController {
    private final TaskService service;
    private final ProjectInternalApi projectApi;

    public TaskController(TaskService service, ProjectInternalApi projectApi) {
        this.service = service;
        this.projectApi = projectApi;
    }

    @GetMapping("/users/{userId}/tasks")
    public List<Task> getAllTasks(
            @PathVariable Long userId
    ) {
        return null;
    }

    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<Task> createTask(
            @PathVariable Long projectId,
            @RequestBody Task task,
            UriComponentsBuilder uriBuilder
    ) {
        Long userId = projectApi.obterUsuarioIdPorProjeto(projectId);
        // The entity is a temporary HTTP input; only creation fields reach the service.
        Task saved = service.create(userId, projectId, task.getTitle(),
                task.getDescription(), task.getDueDate());
        URI location = uriBuilder.path("/api/v1/users/{userId}/tasks/{taskId}")
                .buildAndExpand(userId, saved.getId()).toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @GetMapping("/users/{userId}/tasks/{taskId}")
    public Task getTaskById(
            @PathVariable Long userId,
            @PathVariable Long taskId
    ) {
        return null;
    }

    @PutMapping("/users/{userId}/tasks/{taskId}")
    public Task updateTask(
            @PathVariable Long userId,
            @PathVariable Long taskId,
            @RequestBody Task task
    ) {
        return null;
    }

    @PatchMapping("/users/{userId}/tasks/{taskId}")
    public Task partiallyUpdateTask(
            @PathVariable Long userId,
            @PathVariable Long taskId,
            @RequestBody Task task
    ) {
        return null;
    }

    @DeleteMapping("/users/{userId}/tasks/{taskId}")
    public Void deleteTask(
            @PathVariable Long userId,
            @PathVariable Long taskId
    ) {
        return null;
    }

    @GetMapping("/users/{userId}/projects/{projectId}/tasks")
    public List<Task> getTasksByProject(
            @PathVariable Long userId,
            @PathVariable Long projectId
    ) {
        return null;
    }
}
