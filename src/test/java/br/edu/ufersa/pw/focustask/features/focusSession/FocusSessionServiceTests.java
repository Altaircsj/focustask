package br.edu.ufersa.pw.focustask.features.focusSession;

import br.edu.ufersa.pw.focustask.features.task.TaskInternalApi;
import br.edu.ufersa.pw.focustask.features.user.UserInternalApi;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FocusSessionServiceTests {
    private final FocusSessionRepository repository = mock(FocusSessionRepository.class);
    private final UserInternalApi users = mock(UserInternalApi.class);
    private final TaskInternalApi tasks = mock(TaskInternalApi.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-17T12:00:00Z"), ZoneOffset.UTC);
    private final FocusSessionService service = new FocusSessionService(repository, users, tasks, clock);

    @Test
    void validatesUserAndTaskBeforeCreating() {
        when(users.existePorId(1L)).thenReturn(true);
        when(tasks.pertenceAoUsuario(1L, 7L)).thenReturn(false);
        assertThrows(ResponseStatusException.class, () -> service.create(1L, 7L));
        verifyNoInteractions(repository);
    }

    @Test
    void standaloneCreationDoesNotRequireATask() {
        when(users.existePorId(1L)).thenReturn(true);
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

        FocusSession session = service.create(1L, null);

        assertNull(session.getTaskId());
        assertEquals(FocusSessionStatus.RUNNING, session.getStatus());
        assertEquals(clock.instant(), session.getStartedAt());
        verifyNoInteractions(tasks);
    }

    @Test
    void invalidLinkDoesNotMutateTheLockedSession() {
        FocusSession session = new FocusSession(1L, clock);
        session.setTaskId(5L);
        when(repository.findForUpdate(8L, 1L)).thenReturn(Optional.of(session));
        when(tasks.pertenceAoUsuario(1L, 7L)).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> service.linkTask(1L, 8L, 7L));
        assertEquals(5L, session.getTaskId());
    }

    @Test
    void completedSessionCanBeDetachedWithoutChangingItsHistory() {
        FocusSession session = new FocusSession(1L, clock);
        session.setTaskId(5L);
        session.complete(clock);
        when(repository.findForUpdate(8L, 1L)).thenReturn(Optional.of(session));

        service.linkTask(1L, 8L, null);

        assertNull(session.getTaskId());
        assertEquals(FocusSessionStatus.COMPLETED, session.getStatus());
        assertEquals(clock.instant(), session.getEndedAt());
        verifyNoInteractions(tasks);
    }

    @Test
    void validatesTaskEvenWhenItsSessionListWouldBeEmpty() {
        when(tasks.pertenceAoUsuario(1L, 7L)).thenReturn(false);
        assertThrows(ResponseStatusException.class, () -> service.getByTask(1L, 7L));
        verifyNoInteractions(repository);
    }
}
