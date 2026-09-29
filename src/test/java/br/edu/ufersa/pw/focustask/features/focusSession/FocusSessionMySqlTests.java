package br.edu.ufersa.pw.focustask.features.focusSession;

import br.edu.ufersa.pw.focustask.shared.exception.OperacaoInvalidaException;
import br.edu.ufersa.pw.focustask.shared.exception.EntidadeNaoEncontradaException;
import br.edu.ufersa.pw.focustask.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import br.edu.ufersa.pw.focustask.features.focusSession.dto.*;
import static org.junit.jupiter.api.Assertions.*;

class FocusSessionMySqlTests extends MySqlIntegrationTest {
    @Autowired FocusSessionApplicationService service;
    @Autowired FocusSessionRepository repository;

    @Test
    void supportsStandaloneSessionAndLinkingAfterCompletion() {
        long owner = user("owner@example.com");
        long task = task(project(owner));
        FocusSessionResponseDTO session = service.create(owner, new FocusSessionCreateDTO(null));
        long id = session.id();
        service.patch(owner, id, new FocusSessionPatchDTO(null, FocusSessionStatusDTO.PAUSED));
        service.patch(owner, id, new FocusSessionPatchDTO(null, FocusSessionStatusDTO.RUNNING));
        service.patch(owner, id, new FocusSessionPatchDTO(null, FocusSessionStatusDTO.COMPLETED));
        service.patch(owner, id, new FocusSessionPatchDTO(task, null));
        assertEquals(task, service.getById(owner, id).taskId());
        service.update(owner, id, new FocusSessionUpdateDTO(null, FocusSessionStatusDTO.COMPLETED));

        FocusSession stored = repository.findById(id).orElseThrow();
        assertNull(stored.getTaskId());
        assertEquals(FocusSessionStatus.COMPLETED, stored.getStatus());
        assertNotNull(stored.getEndedAt());
        assertNull(stored.getPausedAt());
        assertEquals("A completed session cannot be resumed",
                assertThrows(OperacaoInvalidaException.class, () -> service.patch(owner, id, new FocusSessionPatchDTO(null, FocusSessionStatusDTO.RUNNING))).getMessage());
    }

    @Test
    void rejectsCrossUserLinksAndScopedReads() {
        long owner = user("owner@example.com");
        long stranger = user("stranger@example.com");
        long task = task(project(stranger));
        long id = service.create(owner, new FocusSessionCreateDTO(null)).id();

        assertEquals("Task not found",
                assertThrows(EntidadeNaoEncontradaException.class, () -> service.patch(owner, id, new FocusSessionPatchDTO(task, null))).getMessage());
        assertEquals("Task not found",
                assertThrows(EntidadeNaoEncontradaException.class, () -> service.createForTask(owner, task)).getMessage());
        assertEquals("Focus session not found",
                assertThrows(EntidadeNaoEncontradaException.class, () -> service.getById(stranger, id)).getMessage());
        assertEquals("Task not found",
                assertThrows(EntidadeNaoEncontradaException.class, () -> service.getByTask(owner, task)).getMessage());
        assertNull(repository.findById(id).orElseThrow().getTaskId());
    }

    @Test
    void databaseRestrictsInvalidLinksAndInvalidSessionStates() {
        long owner = user("owner@example.com");
        long id = service.create(owner, new FocusSessionCreateDTO(null)).id();
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("update focus_sessions set task_id=-1 where id=?", id));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("update focus_sessions set status='PAUSED' where id=?", id));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("update focus_sessions set total_paused_seconds=-1 where id=?", id));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("delete from users where id=?", owner));
    }

    @Test
    void invalidCombinedUpdatePreservesPersistedLinkAndCompletedHistory() {
        long owner = user("combined@example.com");
        long task = task(project(owner));
        long replacement = task(project(owner));
        long id = service.createForTask(owner, task).id();
        service.patch(owner, id, new FocusSessionPatchDTO(null, FocusSessionStatusDTO.COMPLETED));
        var before = jdbc.queryForMap("select * from focus_sessions where id=?", id);
        assertEquals("A completed session cannot be resumed",
                assertThrows(OperacaoInvalidaException.class,
                () -> service.patch(owner, id, new FocusSessionPatchDTO(replacement, FocusSessionStatusDTO.RUNNING))).getMessage());
        assertEquals("A completed session cannot be paused",
                assertThrows(OperacaoInvalidaException.class,
                () -> service.update(owner, id, new FocusSessionUpdateDTO(null, FocusSessionStatusDTO.PAUSED))).getMessage());
        assertEquals(before, jdbc.queryForMap("select * from focus_sessions where id=?", id));
    }

    @Test
    void concurrentPauseRequestsUseOnePersistedPauseStart() throws Exception {
        long owner = user("concurrent@example.com");
        long id = service.create(owner, new FocusSessionCreateDTO(null)).id();
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<FocusSessionResponseDTO> pause = () -> {
                ready.countDown();
                if (!start.await(10, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Start timed out");
                return service.patch(owner, id, new FocusSessionPatchDTO(null, FocusSessionStatusDTO.PAUSED));
            };
            var first = executor.submit(pause);
            var second = executor.submit(pause);
            assertTrue(ready.await(10, java.util.concurrent.TimeUnit.SECONDS));
            start.countDown();
            var a = first.get(20, java.util.concurrent.TimeUnit.SECONDS);
            var b = second.get(20, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(a.pausedAt(), b.pausedAt());
            assertEquals(a.pausedAt(), repository.findById(id).orElseThrow().getPausedAt());
            assertEquals(0, a.totalPausedSeconds());
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }
}
