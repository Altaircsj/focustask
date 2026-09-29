package br.edu.ufersa.pw.focustask.features.project;

import br.edu.ufersa.pw.focustask.features.project.dto.*;
import jakarta.validation.Valid;
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

    @GetMapping("/users/{userId}/projects")
    public ResponseEntity<List<ProjectResponseDTO>> getAll(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getAll(userId));
    }

    @PostMapping("/users/{userId}/projects")
    public ResponseEntity<ProjectResponseDTO> create(
            @PathVariable Long userId,
            @RequestBody @Valid ProjectCreateDTO dto,
            UriComponentsBuilder uriBuilder) {
        ProjectResponseDTO saved = service.create(userId, dto);
        URI location = uriBuilder.path("/api/v1/users/{userId}/projects/{projectId}")
                .buildAndExpand(userId, saved.id()).toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @GetMapping("/users/{userId}/projects/{projectId}")
    public ResponseEntity<ProjectResponseDTO> getById(
            @PathVariable Long userId,
            @PathVariable Long projectId) {
        return ResponseEntity.ok(service.getById(userId, projectId));
    }

    @PutMapping("/users/{userId}/projects/{projectId}")
    public ResponseEntity<ProjectResponseDTO> update(@PathVariable Long userId, @PathVariable Long projectId,
            @RequestBody @Valid ProjectUpdateDTO dto) {
        return ResponseEntity.ok(service.update(userId, projectId, dto));
    }

    @PatchMapping("/users/{userId}/projects/{projectId}")
    public ResponseEntity<ProjectResponseDTO> patch(@PathVariable Long userId, @PathVariable Long projectId,
            @RequestBody @Valid ProjectPatchDTO dto) {
        return ResponseEntity.ok(service.patch(userId, projectId, dto));
    }

    @DeleteMapping("/users/{userId}/projects/{projectId}")
    public ResponseEntity<Void> delete(@PathVariable Long userId, @PathVariable Long projectId) {
        service.delete(userId, projectId);
        return ResponseEntity.noContent().build();
    }
}
