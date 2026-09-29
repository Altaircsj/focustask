package br.edu.ufersa.pw.focustask.features.task.dto;

import java.time.LocalDate;

public record TaskResponseDTO(
        Long id,
        Long projectId,
        String title,
        String description,
        TaskStatusDTO status,
        TaskPriorityDTO priority,
        LocalDate dueDate
) {}
