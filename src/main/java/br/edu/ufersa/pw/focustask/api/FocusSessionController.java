package br.edu.ufersa.pw.focustask.api;

import br.edu.ufersa.pw.focustask.domain.entities.FocusSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/focus-sessions")
public class FocusSessionController {

    @GetMapping
    public List<FocusSession> getAllFocusSessions() {
        return null;
    }

    @PostMapping
    public FocusSession createFocusSession(
            @RequestBody FocusSession focusSession
    ) {
        return null;
    }

    @GetMapping("/{focusSessionId}")
    public FocusSession getFocusSessionById(
            @PathVariable Long focusSessionId
    ) {
        return null;
    }

    @PutMapping("/{focusSessionId}")
    public FocusSession updateFocusSession(
            @PathVariable Long focusSessionId,
            @RequestBody FocusSession focusSession
    ) {
        return null;
    }

    @PatchMapping("/{focusSessionId}")
    public FocusSession partiallyUpdateFocusSession(
            @PathVariable Long focusSessionId,
            @RequestBody FocusSession focusSession
    ) {
        return null;
    }

    @DeleteMapping("/{focusSessionId}")
    public Void deleteFocusSession(
            @PathVariable Long focusSessionId
    ) {
        return null;
    }
}