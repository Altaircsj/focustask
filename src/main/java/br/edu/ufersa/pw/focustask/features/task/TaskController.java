package br.edu.ufersa.pw.focustask.features.task;

import br.edu.ufersa.pw.focustask.features.task.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class TaskController {
    private final TaskApplicationService service;

    public TaskController(TaskApplicationService service) { this.service = service; }

    @GetMapping("/users/{userId}/tasks")
    public ResponseEntity<List<TaskResponseDTO>> getAll(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getAll(userId));
    }

    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponseDTO> create(
            @PathVariable Long projectId,
            @RequestBody @Valid TaskCreateDTO dto,
            UriComponentsBuilder uriBuilder) {
        TaskCreationResult created = service.create(projectId, dto);
        URI location = uriBuilder.path("/api/v1/users/{userId}/tasks/{taskId}")
                .buildAndExpand(created.userId(), created.task().id()).toUri();
        return ResponseEntity.created(location).body(created.task());
    }

    @GetMapping("/users/{userId}/tasks/{taskId}")
    public ResponseEntity<TaskResponseDTO> getById(@PathVariable Long userId, @PathVariable Long taskId) {
        return ResponseEntity.ok(service.getById(userId, taskId));
    }

    @PutMapping("/users/{userId}/tasks/{taskId}")
    public ResponseEntity<TaskResponseDTO> update(@PathVariable Long userId, @PathVariable Long taskId,
            @RequestBody @Valid TaskUpdateDTO dto) {
        return ResponseEntity.ok(service.update(userId, taskId, dto));
    }

    @PatchMapping("/users/{userId}/tasks/{taskId}")
    public ResponseEntity<TaskResponseDTO> patch(@PathVariable Long userId, @PathVariable Long taskId,
            @RequestBody @Valid TaskPatchDTO dto) {
        return ResponseEntity.ok(service.patch(userId, taskId, dto));
    }

    @DeleteMapping("/users/{userId}/tasks/{taskId}")
    public ResponseEntity<Void> delete(@PathVariable Long userId, @PathVariable Long taskId) {
        service.delete(userId, taskId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{userId}/projects/{projectId}/tasks")
    public ResponseEntity<List<TaskResponseDTO>> getByProject(@PathVariable Long userId,
            @PathVariable Long projectId) {
        return ResponseEntity.ok(service.getByProject(userId, projectId));
    }
}
