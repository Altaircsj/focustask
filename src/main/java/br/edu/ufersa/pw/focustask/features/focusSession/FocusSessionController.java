package br.edu.ufersa.pw.focustask.features.focusSession;

import br.edu.ufersa.pw.focustask.features.focusSession.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class FocusSessionController {
    private final FocusSessionApplicationService service;

    public FocusSessionController(FocusSessionApplicationService service) { this.service = service; }

    @GetMapping("/users/{userId}/focus-sessions")
    public ResponseEntity<List<FocusSessionResponseDTO>> getAll(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getAll(userId));
    }

    @PostMapping("/users/{userId}/focus-sessions")
    public ResponseEntity<FocusSessionResponseDTO> create(
            @PathVariable Long userId,
            @RequestBody @Valid FocusSessionCreateDTO dto,
            UriComponentsBuilder uriBuilder) {
        FocusSessionResponseDTO saved = service.create(userId, dto);
        URI location = uriBuilder.path("/api/v1/users/{userId}/focus-sessions/{focusSessionId}")
                .buildAndExpand(userId, saved.id()).toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @GetMapping("/users/{userId}/focus-sessions/{focusSessionId}")
    public ResponseEntity<FocusSessionResponseDTO> getById(
            @PathVariable Long userId,
            @PathVariable Long focusSessionId) {
        return ResponseEntity.ok(service.getById(userId, focusSessionId));
    }

    @PutMapping("/users/{userId}/focus-sessions/{focusSessionId}")
    public ResponseEntity<FocusSessionResponseDTO> update(
            @PathVariable Long userId,
            @PathVariable Long focusSessionId,
            @RequestBody @Valid FocusSessionUpdateDTO dto) {
        return ResponseEntity.ok(service.update(userId, focusSessionId, dto));
    }

    @PatchMapping("/users/{userId}/focus-sessions/{focusSessionId}")
    public ResponseEntity<FocusSessionResponseDTO> patch(
            @PathVariable Long userId,
            @PathVariable Long focusSessionId,
            @RequestBody @Valid FocusSessionPatchDTO dto) {
        return ResponseEntity.ok(service.patch(userId, focusSessionId, dto));
    }

    @DeleteMapping("/users/{userId}/focus-sessions/{focusSessionId}")
    public ResponseEntity<Void> delete(@PathVariable Long userId, @PathVariable Long focusSessionId) {
        service.delete(userId, focusSessionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{userId}/tasks/{taskId}/focus-sessions")
    public ResponseEntity<List<FocusSessionResponseDTO>> getByTask(@PathVariable Long userId,
            @PathVariable Long taskId) {
        return ResponseEntity.ok(service.getByTask(userId, taskId));
    }

    @PostMapping("/users/{userId}/tasks/{taskId}/focus-sessions")
    public ResponseEntity<FocusSessionResponseDTO> createForTask(@PathVariable Long userId,
            @PathVariable Long taskId,
            UriComponentsBuilder uriBuilder) {
        FocusSessionResponseDTO saved = service.createForTask(userId, taskId);
        URI location = uriBuilder.path("/api/v1/users/{userId}/focus-sessions/{focusSessionId}")
                .buildAndExpand(userId, saved.id()).toUri();
        return ResponseEntity.created(location).body(saved);
    }
}
