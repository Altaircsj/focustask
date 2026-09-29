package br.edu.ufersa.pw.focustask.features.focusSession.dto;

import jakarta.validation.constraints.Positive;

public record FocusSessionCreateDTO(
        @Positive Long taskId
) {}
