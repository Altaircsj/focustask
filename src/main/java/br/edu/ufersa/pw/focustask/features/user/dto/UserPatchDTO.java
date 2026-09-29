package br.edu.ufersa.pw.focustask.features.user.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public record UserPatchDTO(
        @Size(min = 1, max = 255)
        @Pattern(regexp = "(?s).*[^\\p{javaWhitespace}].*", message = "must not be blank") String name,
        @Email @Size(min = 1, max = 254) String email
) {}
