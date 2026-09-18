package br.edu.ufersa.pw.focustask.features.user;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController { // Mantido public

    @GetMapping
    public java.util.List<UserDTO> getAllUsers() {
        return null; // TODO: Integrar o service na etapa dos DTOs HTTP.
    }

    @PatchMapping("/{userId}")
    public UserDTO partiallyUpdateUser(@PathVariable Long userId, @RequestBody UserDTO user) {
        return null; // TODO: Distinguir campos ausentes na etapa dos DTOs HTTP.
    }

    @PostMapping
    public User create(@RequestBody User user) {
        return null; // TODO: Implementar usando Service e DTO
    }

    @GetMapping("/{userId}")
    public User getById(@PathVariable Long userId) {
        return null; // TODO
    }

    @PutMapping("/{userId}")
    public User update(@PathVariable Long userId, @RequestBody User user) {
        return null; // TODO
    }

    @DeleteMapping("/{userId}")
    public void delete(@PathVariable Long userId) {
        // TODO: Implementar deleção em cascata via Service
    }
}
