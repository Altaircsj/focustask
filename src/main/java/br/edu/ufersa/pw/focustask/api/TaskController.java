package br.edu.ufersa.pw.focustask.api;

import br.edu.ufersa.pw.focustask.domain.entities.Task;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//TODO com autenticação pode retirar o caminho /users/{userId} de todos aqui

@RestController
@RequestMapping("/api/v1")
public class TaskController {

    @GetMapping("/users/{userId}/tasks")
    public List<Task> getAllTasks(
            @PathVariable Long userId
    ) {
        return null;
    }

    @PostMapping("/users/{userId}/tasks")
    public Task createTask(
            @PathVariable Long userId,
            @RequestBody Task task
    ) {
        return null;
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