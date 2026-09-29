package br.edu.ufersa.pw.focustask.features.user;

import br.edu.ufersa.pw.focustask.features.user.dto.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController { // Mantido public

    @GetMapping
    public java.util.List<UserResponseDTO> getAllUsers() {
        return null; // TODO: Integrar o service na etapa dos casos de uso.
    }

    @PatchMapping("/{userId}")
    public UserResponseDTO partiallyUpdateUser(@PathVariable Long userId, @RequestBody @Valid UserPatchDTO user) {
        return null; // TODO: Integrar PATCH na etapa dos casos de uso; null não altera.
    }

    @PostMapping
    public UserResponseDTO create(@RequestBody @Valid UserCreateDTO user) {
        return null; // TODO: Integrar o service na etapa dos casos de uso
    }

    @GetMapping("/{userId}")
    public UserResponseDTO getById(@PathVariable Long userId) {
        return null; // TODO
    }

    @PutMapping("/{userId}")
    public UserResponseDTO update(@PathVariable Long userId, @RequestBody @Valid UserUpdateDTO user) {
        return null; // TODO
    }

    @DeleteMapping("/{userId}")
    public void delete(@PathVariable Long userId) {
        // TODO: Implementar deleção em cascata via Service
    }
}
