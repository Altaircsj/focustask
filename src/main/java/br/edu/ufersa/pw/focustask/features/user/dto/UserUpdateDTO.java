package br.edu.ufersa.pw.focustask.features.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;

public record UserUpdateDTO(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email @Size(max = 254) String email
) {}
