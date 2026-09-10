package br.edu.ufersa.pw.focustask.api;

import br.edu.ufersa.pw.focustask.domain.entities.FocusSession;
import br.edu.ufersa.pw.focustask.domain.entities.Task;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    @GetMapping
    public List<Task> getAllTasks() {
        return null;
    }

    @PostMapping
    public Task createTask(@RequestBody Task task) {
        return null;
    }

    @GetMapping("/{taskId}")
    public Task getTaskById(@PathVariable Long taskId) {
        return null;
    }

    @PutMapping("/{taskId}")
    public Task updateTask(
            @PathVariable Long taskId,
            @RequestBody Task task
    ) {
        return null;
    }

    @PatchMapping("/{taskId}")
    public Task partiallyUpdateTask(
            @PathVariable Long taskId,
            @RequestBody Task task
    ) {
        return null;
    }

    @DeleteMapping("/{taskId}")
    public Void deleteTask(@PathVariable Long taskId) {
        return null;
    }

    @GetMapping("/{taskId}/focus-sessions")
    public List<FocusSession> getTaskFocusSessions(
            @PathVariable Long taskId
    ) {
        return null;
    }
}