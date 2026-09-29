package br.edu.ufersa.pw.focustask.features.focusSession.dto;

import jakarta.validation.constraints.Positive;

public record FocusSessionPatchDTO(
        @Positive Long taskId,
        FocusSessionStatusDTO status
) {}
