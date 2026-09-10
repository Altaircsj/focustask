package br.edu.ufersa.pw.focustask.api;

import br.edu.ufersa.pw.focustask.domain.entities.FocusSession;
import br.edu.ufersa.pw.focustask.domain.entities.Project;
import br.edu.ufersa.pw.focustask.domain.entities.Task;
import br.edu.ufersa.pw.focustask.domain.entities.User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @GetMapping
    public List<User> getAllUsers() {
        return null;
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        return null;
    }

    @GetMapping("/{userId}")
    public User getUserById(@PathVariable Long userId) {
        return null;
    }

    @PutMapping("/{userId}")
    public User updateUser(
            @PathVariable Long userId,
            @RequestBody User user
    ) {
        return null;
    }

    @PatchMapping("/{userId}")
    public User partiallyUpdateUser(
            @PathVariable Long userId,
            @RequestBody User user
    ) {
        return null;
    }

    @DeleteMapping("/{userId}")
    public Void deleteUser(@PathVariable Long userId) {
        return null;
    }

    @GetMapping("/{userId}/projects")
    public List<Project> getUserProjects(@PathVariable Long userId) {
        return null;
    }

    @GetMapping("/{userId}/tasks")
    public List<Task> getUserTasks(@PathVariable Long userId) {
        return null;
    }

    @GetMapping("/{userId}/focus-sessions")
    public List<FocusSession> getUserFocusSessions(
            @PathVariable Long userId
    ) {
        return null;
    }
}