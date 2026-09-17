package br.edu.ufersa.pw.focustask.features.project;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users/{userId}/projects")
public class ProjectController {

    @PostMapping
    public Project create(@PathVariable Long userId, @RequestBody Project project) {
        return null; // TODO: Implementar via service
    }

    @GetMapping
    public List<Project> getAllByUser(@PathVariable Long userId) {
        return null; // TODO
    }

    @GetMapping("/{projectId}")
    public Project getById(@PathVariable Long userId, @PathVariable Long projectId) {
        return null; // TODO
    }

    @PutMapping("/{projectId}")
    public Project update(@PathVariable Long userId, @PathVariable Long projectId, @RequestBody Project project) {
        return null; // TODO
    }

    @DeleteMapping("/{projectId}")
    public void delete(@PathVariable Long userId, @PathVariable Long projectId) {
        // TODO: Implementar regras transacionais
    }
}