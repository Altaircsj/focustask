package br.edu.ufersa.pw.focustask.features.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record TaskUpdateDTO(
        @NotNull @Positive Long projectId,
        @NotBlank @Size(max = 255) String title,
        String description,
        @NotNull TaskStatusDTO status,
        @NotNull TaskPriorityDTO priority,
        LocalDate dueDate
) {}
