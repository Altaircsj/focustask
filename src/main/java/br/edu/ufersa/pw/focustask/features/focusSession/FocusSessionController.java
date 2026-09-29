package br.edu.ufersa.pw.focustask.features.focusSession;

import br.edu.ufersa.pw.focustask.features.focusSession.dto.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//TODO com autenticação pode retirar o caminho /users/{userId} de todos aqui

@RestController
@RequestMapping("/api/v1")
public class FocusSessionController {

    @GetMapping("/users/{userId}/focus-sessions")
    public List<FocusSessionResponseDTO> getAllFocusSessions(
            @PathVariable Long userId
    ) {
        return null;
    }

    @PostMapping("/users/{userId}/focus-sessions")
    public FocusSessionResponseDTO createFocusSession(
            @PathVariable Long userId,
            @RequestBody @Valid FocusSessionCreateDTO focusSession
    ) {
        return null;
    }

    @GetMapping("/users/{userId}/focus-sessions/{focusSessionId}")
    public FocusSessionResponseDTO getFocusSessionById(
            @PathVariable Long userId,
            @PathVariable Long focusSessionId
    ) {
        return null;
    }

    @PutMapping("/users/{userId}/focus-sessions/{focusSessionId}")
    public FocusSessionResponseDTO updateFocusSession(
            @PathVariable Long userId,
            @PathVariable Long focusSessionId,
            @RequestBody @Valid FocusSessionUpdateDTO focusSession
    ) {
        return null;
    }

    @PatchMapping("/users/{userId}/focus-sessions/{focusSessionId}")
    public FocusSessionResponseDTO partiallyUpdateFocusSession(
            @PathVariable Long userId,
            @PathVariable Long focusSessionId,
            @RequestBody @Valid FocusSessionPatchDTO focusSession
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
    public List<FocusSessionResponseDTO> getFocusSessionsByTask(
            @PathVariable Long userId,
            @PathVariable Long taskId
    ) {
        return null;
    }

    @PostMapping("/users/{userId}/tasks/{taskId}/focus-sessions")
    public FocusSessionResponseDTO createFocusSessionForTask(
            @PathVariable Long userId,
            @PathVariable Long taskId
    ) {
        return null;
    }
}
