package br.edu.ufersa.pw.focustask.features.user;

import br.edu.ufersa.pw.focustask.features.user.dto.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import br.edu.ufersa.pw.focustask.shared.security.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class UserController {
    private final UserApplicationService service;

    public UserController(UserApplicationService service) { this.service = service; }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponseDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getById(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(service.getById(principal.getId()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponseDTO> update(@AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody @Valid UserUpdateDTO dto) {
        return ResponseEntity.ok(service.update(principal.getId(), dto));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponseDTO> patch(@AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody @Valid UserPatchDTO dto) {
        return ResponseEntity.ok(service.patch(principal.getId(), dto));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser principal) {
        service.delete(principal.getId());
        return ResponseEntity.noContent().build();
    }
}
