package br.edu.ufersa.pw.focustask.features.project;

import br.edu.ufersa.pw.focustask.features.project.dto.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users/{userId}/projects")
public class ProjectController {

    @PatchMapping("/{projectId}")
    public ProjectResponseDTO partiallyUpdateProject(@PathVariable Long userId,
                                            @PathVariable Long projectId,
                                            @RequestBody @Valid ProjectPatchDTO project) {
        return null; // TODO: Integrar PATCH na etapa dos casos de uso; null não altera.
    }

    @PostMapping
    public ProjectResponseDTO create(@PathVariable Long userId, @RequestBody @Valid ProjectCreateDTO project) {
        return null; // TODO: Implementar via service
    }

    @GetMapping
    public List<ProjectResponseDTO> getAllByUser(@PathVariable Long userId) {
        return null; // TODO
    }

    @GetMapping("/{projectId}")
    public ProjectResponseDTO getById(@PathVariable Long userId, @PathVariable Long projectId) {
        return null; // TODO
    }

    @PutMapping("/{projectId}")
    public ProjectResponseDTO update(@PathVariable Long userId, @PathVariable Long projectId, @RequestBody @Valid ProjectUpdateDTO project) {
        return null; // TODO
    }

    @DeleteMapping("/{projectId}")
    public void delete(@PathVariable Long userId, @PathVariable Long projectId) {
        // TODO: Implementar regras transacionais
    }
}
