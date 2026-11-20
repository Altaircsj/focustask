package br.edu.ufersa.pw.focustask.features.focusSession.dto;

import jakarta.validation.constraints.Positive;

/** Supplying an association with both IDs absent detaches the session. */
public record FocusSessionAssociationDTO(@Positive Long taskId, @Positive Long projectId) {}
