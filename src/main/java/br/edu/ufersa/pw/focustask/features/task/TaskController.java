package br.edu.ufersa.pw.focustask.features.task;

import br.edu.ufersa.pw.focustask.features.project.ProjectInternalApi;
import org.springframework.http.ResponseEntity;
import br.edu.ufersa.pw.focustask.features.task.dto.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

//TODO com autenticação pode retirar o caminho /users/{userId} de todos aqui

@RestController
@RequestMapping("/api/v1")
public class TaskController {
    private final TaskService service;
    private final TaskMapper mapper;
    private final ProjectInternalApi projectApi;

    public TaskController(TaskService service, ProjectInternalApi projectApi, TaskMapper mapper) {
        this.service = service;
        this.mapper = mapper;
        this.projectApi = projectApi;
    }

    @GetMapping("/users/{userId}/tasks")
    public List<TaskResponseDTO> getAllTasks(
            @PathVariable Long userId
    ) {
        return null;
    }

    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponseDTO> createTask(
            @PathVariable Long projectId,
            @RequestBody @Valid TaskCreateDTO task,
            UriComponentsBuilder uriBuilder
    ) {
        Long userId = projectApi.obterUsuarioIdPorProjeto(projectId);
        Task saved = service.create(userId, projectId, task.title(),
                task.description(), task.dueDate());
        URI location = uriBuilder.path("/api/v1/users/{userId}/tasks/{taskId}")
                .buildAndExpand(userId, saved.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    @GetMapping("/users/{userId}/tasks/{taskId}")
    public TaskResponseDTO getTaskById(
            @PathVariable Long userId,
            @PathVariable Long taskId
    ) {
        return null;
    }

    @PutMapping("/users/{userId}/tasks/{taskId}")
    public TaskResponseDTO updateTask(
            @PathVariable Long userId,
            @PathVariable Long taskId,
            @RequestBody @Valid TaskUpdateDTO task
    ) {
        return null;
    }

    @PatchMapping("/users/{userId}/tasks/{taskId}")
    public TaskResponseDTO partiallyUpdateTask(
            @PathVariable Long userId,
            @PathVariable Long taskId,
            @RequestBody @Valid TaskPatchDTO task
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
    public List<TaskResponseDTO> getTasksByProject(
            @PathVariable Long userId,
            @PathVariable Long projectId
    ) {
        return null;
    }
}
