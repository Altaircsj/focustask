package br.edu.ufersa.pw.focustask.features.project;

import br.edu.ufersa.pw.focustask.features.project.dto.*;
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
public class ProjectController {
    private final ProjectApplicationService service;

    public ProjectController(ProjectApplicationService service) { this.service = service; }

    @GetMapping("/projects")
    public ResponseEntity<List<ProjectResponseDTO>> getAll(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(service.getAll(principal.getId()));
    }

    @PostMapping("/projects")
    public ResponseEntity<ProjectResponseDTO> create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody @Valid ProjectCreateDTO dto,
            UriComponentsBuilder uriBuilder) {
        ProjectResponseDTO saved = service.create(principal.getId(), dto);
        URI location = uriBuilder.path("/api/v1/projects/{projectId}")
                .buildAndExpand(saved.id()).toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @GetMapping("/projects/{projectId}")
    public ResponseEntity<ProjectResponseDTO> getById(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long projectId) {
        return ResponseEntity.ok(service.getById(principal.getId(), projectId));
    }

    @PutMapping("/projects/{projectId}")
    public ResponseEntity<ProjectResponseDTO> update(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long projectId,
            @RequestBody @Valid ProjectUpdateDTO dto) {
        return ResponseEntity.ok(service.update(principal.getId(), projectId, dto));
    }

    @PatchMapping("/projects/{projectId}")
    public ResponseEntity<ProjectResponseDTO> patch(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long projectId,
            @RequestBody @Valid ProjectPatchDTO dto) {
        return ResponseEntity.ok(service.patch(principal.getId(), projectId, dto));
    }

    @DeleteMapping("/projects/{projectId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long projectId) {
        service.delete(principal.getId(), projectId);
        return ResponseEntity.noContent().build();
    }
}
