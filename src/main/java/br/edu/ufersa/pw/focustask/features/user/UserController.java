package br.edu.ufersa.pw.focustask.features.user;

import br.edu.ufersa.pw.focustask.features.user.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
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

    @PostMapping("/users")
    public ResponseEntity<UserResponseDTO> create(@RequestBody @Valid UserCreateDTO dto,
            UriComponentsBuilder uriBuilder) {
        UserResponseDTO saved = service.create(dto);
        URI location = uriBuilder.path("/api/v1/users/{userId}")
                .buildAndExpand(saved.id()).toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponseDTO> getById(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getById(userId));
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<UserResponseDTO> update(@PathVariable Long userId,
            @RequestBody @Valid UserUpdateDTO dto) {
        return ResponseEntity.ok(service.update(userId, dto));
    }

    @PatchMapping("/users/{userId}")
    public ResponseEntity<UserResponseDTO> patch(@PathVariable Long userId,
            @RequestBody @Valid UserPatchDTO dto) {
        return ResponseEntity.ok(service.patch(userId, dto));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> delete(@PathVariable Long userId) {
        service.delete(userId);
        return ResponseEntity.noContent().build();
    }
}
