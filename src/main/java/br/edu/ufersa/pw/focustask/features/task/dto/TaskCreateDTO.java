package br.edu.ufersa.pw.focustask.features.task.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskCreateDTO(
        @NotBlank @Size(max = 255) String title,
        String description,
        LocalDate dueDate
) {}
