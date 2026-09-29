package br.edu.ufersa.pw.focustask.features.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectUpdateDTO(
        @NotBlank @Size(max = 255) String name,
        String description
) {}
