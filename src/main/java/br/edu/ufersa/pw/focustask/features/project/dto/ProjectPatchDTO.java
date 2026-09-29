package br.edu.ufersa.pw.focustask.features.project.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record ProjectPatchDTO(
        @Size(min = 1, max = 255)
        @Pattern(regexp = "(?s).*[^\\p{javaWhitespace}].*", message = "must not be blank") String name,
        String description
) {}
