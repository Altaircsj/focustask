package br.edu.ufersa.pw.focustask.features.task.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record TaskPatchDTO(
        @Positive Long projectId,
        @Size(min = 1, max = 255)
        @Pattern(regexp = "(?s).*[^\\p{javaWhitespace}].*", message = "must not be blank") String title,
        String description,
        TaskStatusDTO status,
        TaskPriorityDTO priority,
        LocalDate dueDate
) {}
