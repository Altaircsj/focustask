package br.edu.ufersa.pw.focustask.features.focusSession;

import br.edu.ufersa.pw.focustask.features.focusSession.dto.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FocusSessionMapperTests {
    private final FocusSessionMapper mapper = Mappers.getMapper(FocusSessionMapper.class);
    private final Instant start = Instant.parse("2026-09-17T12:00:00Z");

    @Test
    void representsStandalonePausedAndCompletedSessionsWithoutMutatingThem() {
        FocusSession session = new FocusSession(7L, at(0));
        ReflectionTestUtils.setField(session, "id", 8L);
        assertEquals(new FocusSessionResponseDTO(8L, 7L, null, FocusSessionStatusDTO.RUNNING,
                start, null, null, 0), mapper.toResponse(session));
        session.setTaskId(123L);
        session.pause(at(60));
        var paused = mapper.toResponse(session);
        assertEquals(FocusSessionStatusDTO.PAUSED, paused.status());
        assertEquals(start.plusSeconds(60), paused.pausedAt());
        assertEquals(0, paused.totalPausedSeconds());
        assertEquals(FocusSessionStatus.PAUSED, session.getStatus());
        session.complete(at(90));
        var completed = mapper.toResponse(session);
        assertEquals(new FocusSessionResponseDTO(8L, 7L, 123L, FocusSessionStatusDTO.COMPLETED,
                start, start.plusSeconds(90), null, 30), completed);
        assertEquals(List.of(completed), mapper.toResponseList(List.of(session)));
        assertEquals(30, session.getTotalPausedSeconds());
        assertEquals(FocusSessionStatus.COMPLETED, session.getStatus());
    }

    private Clock at(long seconds) {
        return Clock.fixed(start.plusSeconds(seconds), ZoneOffset.UTC);
    }
}
