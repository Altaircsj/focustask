package br.edu.ufersa.pw.focustask.features.task;

import br.edu.ufersa.pw.focustask.features.task.dto.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import br.edu.ufersa.pw.focustask.shared.security.AuthenticatedUser;
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

    @GetMapping("/tasks")
    public ResponseEntity<List<TaskResponseDTO>> getAll(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(service.getAll(principal.getId()));
    }

    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponseDTO> create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long projectId,
            @RequestBody @Valid TaskCreateDTO dto,
            UriComponentsBuilder uriBuilder) {
        TaskResponseDTO created = service.create(principal.getId(), projectId, dto);
        URI location = uriBuilder.path("/api/v1/tasks/{taskId}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<TaskResponseDTO> getById(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long taskId) {
        return ResponseEntity.ok(service.getById(principal.getId(), taskId));
    }

    @PutMapping("/tasks/{taskId}")
    public ResponseEntity<TaskResponseDTO> update(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long taskId,
            @RequestBody @Valid TaskUpdateDTO dto) {
        return ResponseEntity.ok(service.update(principal.getId(), taskId, dto));
    }

    @PatchMapping("/tasks/{taskId}")
    public ResponseEntity<TaskResponseDTO> patch(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long taskId,
            @RequestBody @Valid TaskPatchDTO dto) {
        return ResponseEntity.ok(service.patch(principal.getId(), taskId, dto));
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long taskId) {
        service.delete(principal.getId(), taskId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/projects/{projectId}/tasks")
    public ResponseEntity<List<TaskResponseDTO>> getByProject(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long projectId) {
        return ResponseEntity.ok(service.getByProject(principal.getId(), projectId));
    }
}
