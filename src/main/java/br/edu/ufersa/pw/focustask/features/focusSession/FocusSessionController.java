package br.edu.ufersa.pw.focustask.features.focusSession;

import br.edu.ufersa.pw.focustask.features.focusSession.dto.*;
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
public class FocusSessionController {
    private final FocusSessionApplicationService service;

    public FocusSessionController(FocusSessionApplicationService service) { this.service = service; }

    @GetMapping("/focus-sessions")
    public ResponseEntity<List<FocusSessionResponseDTO>> getAll(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(service.getAll(principal.getId()));
    }

    @PostMapping("/focus-sessions")
    public ResponseEntity<FocusSessionResponseDTO> create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody @Valid FocusSessionCreateDTO dto,
            UriComponentsBuilder uriBuilder) {
        FocusSessionResponseDTO saved = service.create(principal.getId(), dto);
        URI location = uriBuilder.path("/api/v1/focus-sessions/{focusSessionId}")
                .buildAndExpand(saved.id()).toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @GetMapping("/focus-sessions/{focusSessionId}")
    public ResponseEntity<FocusSessionResponseDTO> getById(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long focusSessionId) {
        return ResponseEntity.ok(service.getById(principal.getId(), focusSessionId));
    }

    @PutMapping("/focus-sessions/{focusSessionId}")
    public ResponseEntity<FocusSessionResponseDTO> update(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long focusSessionId,
            @RequestBody @Valid FocusSessionUpdateDTO dto) {
        return ResponseEntity.ok(service.update(principal.getId(), focusSessionId, dto));
    }

    @PatchMapping("/focus-sessions/{focusSessionId}")
    public ResponseEntity<FocusSessionResponseDTO> patch(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long focusSessionId,
            @RequestBody @Valid FocusSessionPatchDTO dto) {
        return ResponseEntity.ok(service.patch(principal.getId(), focusSessionId, dto));
    }

    @DeleteMapping("/focus-sessions/{focusSessionId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long focusSessionId) {
        service.delete(principal.getId(), focusSessionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tasks/{taskId}/focus-sessions")
    public ResponseEntity<List<FocusSessionResponseDTO>> getByTask(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long taskId) {
        return ResponseEntity.ok(service.getByTask(principal.getId(), taskId));
    }

    @PostMapping("/tasks/{taskId}/focus-sessions")
    public ResponseEntity<FocusSessionResponseDTO> createForTask(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long taskId,
            UriComponentsBuilder uriBuilder) {
        FocusSessionResponseDTO saved = service.createForTask(principal.getId(), taskId);
        URI location = uriBuilder.path("/api/v1/focus-sessions/{focusSessionId}")
                .buildAndExpand(saved.id()).toUri();
        return ResponseEntity.created(location).body(saved);
    }
}
