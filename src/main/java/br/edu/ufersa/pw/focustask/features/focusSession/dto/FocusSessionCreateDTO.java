package br.edu.ufersa.pw.focustask.features.focusSession.dto;

import jakarta.validation.constraints.*;

public record FocusSessionCreateDTO(
        @Positive Long taskId,
        @Positive Long projectId,
        @Size(max = 255) String title,
        @Min(1) @Max(240) Integer plannedMinutes
) {
    public FocusSessionCreateDTO(Long taskId) { this(taskId, null, null, null); }
}
