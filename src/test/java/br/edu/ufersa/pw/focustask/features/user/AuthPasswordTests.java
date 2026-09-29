package br.edu.ufersa.pw.focustask.features.user;

import br.edu.ufersa.pw.focustask.features.user.dto.*;
import br.edu.ufersa.pw.focustask.shared.security.AuthenticatedUser;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AuthPasswordTests {
    @Test void passwordLimitsCountUtf8BytesAndDtoTextDoesNotRevealSecrets() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (String valid : List.of("123456", "a".repeat(72), "é".repeat(36))) {
                assertTrue(validator.validate(new RegisterRequestDTO("User", "u@example.com", valid)).isEmpty());
            }
            for (String invalid : List.of("12345", " ", "a".repeat(73), "é".repeat(37))) {
                assertTrue(validator.validate(new RegisterRequestDTO("User", "u@example.com", invalid)).stream()
                        .anyMatch(v -> v.getPropertyPath().toString().equals("password")));
            }
            assertTrue(validator.validate(new LoginRequestDTO("u@example.com", "x")).isEmpty());
            assertFalse(validator.validate(new LoginRequestDTO("u@example.com", "é".repeat(37))).isEmpty());
        }
        assertFalse(new RegisterRequestDTO("User", "u@example.com", "private-password").toString().contains("private-password"));
        assertFalse(new LoginRequestDTO("u@example.com", "private-password").toString().contains("private-password"));
        assertFalse(new TokenResponseDTO("private-token").toString().contains("private-token"));
        var principal = new AuthenticatedUser(1L, "u@example.com", "private-hash", List.of());
        assertFalse(principal.toString().contains("private-hash"));
        principal.eraseCredentials();
        assertNull(principal.getPassword());
    }
}
