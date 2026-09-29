package br.edu.ufersa.pw.focustask.features.focusSession.dto;

import java.time.Instant;

public record FocusSessionResponseDTO(
        Long id,
        Long userId,
        Long taskId,
        FocusSessionStatusDTO status,
        Instant startedAt,
        Instant endedAt,
        Instant pausedAt,
        long totalPausedSeconds
) {}
