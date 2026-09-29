package br.edu.ufersa.pw.focustask.features.focusSession.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FocusSessionUpdateDTO(
        @Positive Long taskId,
        @NotNull FocusSessionStatusDTO status
) {}
