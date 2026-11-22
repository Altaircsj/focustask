package br.edu.ufersa.pw.focustask.features.focusSession.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record FocusSessionPatchDTO(
        @Positive Long taskId,
        FocusSessionStatusDTO status,
        @Size(max = 255) String title,
        @Min(1) @Max(240) Integer plannedMinutes,
        @Valid FocusSessionAssociationDTO association
) {
    public FocusSessionPatchDTO(Long taskId, FocusSessionStatusDTO status) {
        this(taskId, status, null, null, null);
    }
}
