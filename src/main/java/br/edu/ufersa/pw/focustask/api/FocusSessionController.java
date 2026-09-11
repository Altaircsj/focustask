package br.edu.ufersa.pw.focustask.api;

import br.edu.ufersa.pw.focustask.domain.entities.FocusSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//TODO com autenticação pode retirar o caminho /users/{userId} de todos aqui

@RestController
@RequestMapping("/api/v1")
public class FocusSessionController {

    @GetMapping("/users/{userId}/focus-sessions")
    public List<FocusSession> getAllFocusSessions(
            @PathVariable Long userId
    ) {
        return null;
    }

    @PostMapping("/users/{userId}/focus-sessions")
    public FocusSession createFocusSession(
            @PathVariable Long userId,
            @RequestBody FocusSession focusSession
    ) {
        return null;
    }

    @GetMapping("/users/{userId}/focus-sessions/{focusSessionId}")
    public FocusSession getFocusSessionById(
            @PathVariable Long userId,
            @PathVariable Long focusSessionId
    ) {
        return null;
    }

    @PutMapping("/users/{userId}/focus-sessions/{focusSessionId}")
    public FocusSession updateFocusSession(
            @PathVariable Long userId,
            @PathVariable Long focusSessionId,
            @RequestBody FocusSession focusSession
    ) {
        return null;
    }

    @PatchMapping("/users/{userId}/focus-sessions/{focusSessionId}")
    public FocusSession partiallyUpdateFocusSession(
            @PathVariable Long userId,
            @PathVariable Long focusSessionId,
            @RequestBody FocusSession focusSession
    ) {
        return null;
    }

    @DeleteMapping("/users/{userId}/focus-sessions/{focusSessionId}")
    public void deleteFocusSession(
            @PathVariable Long userId,
            @PathVariable Long focusSessionId
    ) {
    }

    @GetMapping("/users/{userId}/tasks/{taskId}/focus-sessions")
    public List<FocusSession> getFocusSessionsByTask(
            @PathVariable Long userId,
            @PathVariable Long taskId
    ) {
        return null;
    }

    @PostMapping("/users/{userId}/tasks/{taskId}/focus-sessions")
    public FocusSession createFocusSessionForTask(
            @PathVariable Long userId,
            @PathVariable Long taskId,
            @RequestBody FocusSession focusSession
    ) {
        return null;
    }
}
