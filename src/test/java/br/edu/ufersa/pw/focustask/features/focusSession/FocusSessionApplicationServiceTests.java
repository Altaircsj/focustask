package br.edu.ufersa.pw.focustask.features.focusSession;

import br.edu.ufersa.pw.focustask.shared.exception.OperacaoInvalidaException;
import br.edu.ufersa.pw.focustask.shared.exception.EntidadeNaoEncontradaException;
import br.edu.ufersa.pw.focustask.features.task.TaskInternalApi;
import br.edu.ufersa.pw.focustask.features.user.UserInternalApi;
import org.junit.jupiter.api.Test;
import br.edu.ufersa.pw.focustask.features.focusSession.dto.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FocusSessionApplicationServiceTests {
    private final FocusSessionRepository repository = mock(FocusSessionRepository.class);
    private final UserInternalApi users = mock(UserInternalApi.class);
    private final TaskInternalApi tasks = mock(TaskInternalApi.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-17T12:00:00Z"), ZoneOffset.UTC);
    private final FocusSessionApplicationService service = new FocusSessionApplicationService(repository, new FocusSessionMapperImpl(), new FocusSessionService(tasks), users, tasks, clock);

    @Test
    void validatesUserAndTaskBeforeCreating() {
        when(users.existePorId(1L)).thenReturn(true);
        when(tasks.pertenceAoUsuario(1L, 7L)).thenReturn(false);
        assertEquals("Task not found",
                assertThrows(EntidadeNaoEncontradaException.class, () -> service.createForTask(1L, 7L)).getMessage());
        verifyNoInteractions(repository);
    }

    @Test
    void standaloneCreationDoesNotRequireATask() {
        when(users.existePorId(1L)).thenReturn(true);
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

        FocusSessionResponseDTO session = service.create(1L, new FocusSessionCreateDTO(null));

        assertNull(session.taskId());
        assertEquals(FocusSessionStatusDTO.RUNNING, session.status());
        assertEquals(clock.instant(), session.startedAt());
        verifyNoInteractions(tasks);
    }

    @Test
    void invalidLinkDoesNotMutateTheLockedSession() {
        FocusSession session = new FocusSession(1L, clock);
        session.setTaskId(5L);
        when(repository.findForUpdate(8L, 1L)).thenReturn(Optional.of(session));
        when(tasks.pertenceAoUsuario(1L, 7L)).thenReturn(false);

        assertEquals("Task not found",
                assertThrows(EntidadeNaoEncontradaException.class, () -> service.patch(1L, 8L, new FocusSessionPatchDTO(7L, null))).getMessage());
        assertEquals(5L, session.getTaskId());
    }

    @Test
    void completedSessionCanBeDetachedWithoutChangingItsHistory() {
        FocusSession session = new FocusSession(1L, clock);
        session.setTaskId(5L);
        session.complete(clock);
        when(repository.findForUpdate(8L, 1L)).thenReturn(Optional.of(session));

        service.update(1L, 8L, new FocusSessionUpdateDTO(null, FocusSessionStatusDTO.COMPLETED));

        assertNull(session.getTaskId());
        assertEquals(FocusSessionStatus.COMPLETED, session.getStatus());
        assertEquals(clock.instant(), session.getEndedAt());
        verifyNoInteractions(tasks);
    }

    @Test
    void validatesTaskEvenWhenItsSessionListWouldBeEmpty() {
        when(tasks.pertenceAoUsuario(1L, 7L)).thenReturn(false);
        assertEquals("Task not found",
                assertThrows(EntidadeNaoEncontradaException.class, () -> service.getByTask(1L, 7L)).getMessage());
        verifyNoInteractions(repository);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
            "RUNNING,RUNNING", "RUNNING,PAUSED", "RUNNING,COMPLETED",
            "PAUSED,RUNNING", "PAUSED,PAUSED", "PAUSED,COMPLETED",
            "COMPLETED,RUNNING", "COMPLETED,PAUSED", "COMPLETED,COMPLETED"
    })
    void appliesTheExistingTransitionMatrixWithoutDoubleCounting(FocusSessionStatus initial,
                                                                FocusSessionStatusDTO desired) {
        FocusSession session = new FocusSession(1L, Clock.offset(clock, java.time.Duration.ofSeconds(-120)));
        if (initial == FocusSessionStatus.PAUSED) session.pause(Clock.offset(clock, java.time.Duration.ofSeconds(-60)));
        if (initial == FocusSessionStatus.COMPLETED) session.complete(Clock.offset(clock, java.time.Duration.ofSeconds(-30)));
        when(repository.findForUpdate(8L, 1L)).thenReturn(Optional.of(session));
        var dto = new FocusSessionPatchDTO(null, desired);
        if (initial == FocusSessionStatus.COMPLETED && desired != FocusSessionStatusDTO.COMPLETED) {
            assertEquals("A completed session cannot be "
                    + (desired == FocusSessionStatusDTO.PAUSED ? "paused" : "resumed"),
                assertThrows(OperacaoInvalidaException.class, () -> service.patch(1L, 8L, dto)).getMessage());
            assertEquals(clock.instant().minusSeconds(30), session.getEndedAt());
        } else {
            var result = service.patch(1L, 8L, dto);
            assertEquals(desired, result.status());
            assertEquals(clock.instant().minusSeconds(120), result.startedAt());
            if (initial == FocusSessionStatus.PAUSED && desired != FocusSessionStatusDTO.PAUSED) {
                assertEquals(60, result.totalPausedSeconds());
                assertNull(result.pausedAt());
            }
            assertEquals(result, service.patch(1L, 8L, dto));
        }
    }

    @Test
    void invalidTransitionDoesNotChangeALinkAndNullPatchDoesNotDetach() {
        FocusSession session = new FocusSession(1L, clock);
        session.setTaskId(5L);
        session.complete(clock);
        when(repository.findForUpdate(8L, 1L)).thenReturn(Optional.of(session));
        when(tasks.pertenceAoUsuario(1L, 7L)).thenReturn(true);
        assertEquals("A completed session cannot be resumed",
                assertThrows(OperacaoInvalidaException.class, () -> service.patch(1L, 8L,
                new FocusSessionPatchDTO(7L, FocusSessionStatusDTO.RUNNING))).getMessage());
        assertEquals("A completed session cannot be paused",
                assertThrows(OperacaoInvalidaException.class, () -> service.update(1L, 8L,
                new FocusSessionUpdateDTO(null, FocusSessionStatusDTO.PAUSED))).getMessage());
        assertEquals(5L, session.getTaskId());
        assertEquals(5L, service.patch(1L, 8L, new FocusSessionPatchDTO(null, null)).taskId());
        assertEquals(7L, service.patch(1L, 8L, new FocusSessionPatchDTO(7L, null)).taskId());
        verify(repository, times(4)).findForUpdate(8L, 1L);
    }

    @Test
    void rejectsMissingUserAndScopedSessionBeforeWriting() {
        assertEquals("User not found",
                assertThrows(EntidadeNaoEncontradaException.class,
                () -> service.create(1L, new FocusSessionCreateDTO(null))).getMessage());
        assertEquals("User not found",
                assertThrows(EntidadeNaoEncontradaException.class,
                () -> service.getAll(1L)).getMessage());
        assertEquals("Focus session not found",
                assertThrows(EntidadeNaoEncontradaException.class, () -> service.getById(2L, 8L)).getMessage());
        assertEquals("Focus session not found",
                assertThrows(EntidadeNaoEncontradaException.class,
                () -> service.patch(2L, 8L, new FocusSessionPatchDTO(7L, null))).getMessage());
        assertEquals("Focus session not found",
                assertThrows(EntidadeNaoEncontradaException.class, () -> service.delete(2L, 8L)).getMessage());
        verifyNoInteractions(tasks);
        verify(repository, never()).save(any());
        verify(repository, never()).delete(any());
    }
}
