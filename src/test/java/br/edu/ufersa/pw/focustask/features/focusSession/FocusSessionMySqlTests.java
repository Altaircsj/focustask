package br.edu.ufersa.pw.focustask.features.focusSession;

import br.edu.ufersa.pw.focustask.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class FocusSessionMySqlTests extends MySqlIntegrationTest {
    @Autowired FocusSessionService service;
    @Autowired FocusSessionRepository repository;

    @Test
    void supportsStandaloneSessionAndLinkingAfterCompletion() {
        long owner = user("owner@example.com");
        long task = task(project(owner));
        FocusSession session = service.create(owner, null);
        long id = session.getId();
        service.pause(owner, id);
        service.resume(owner, id);
        service.complete(owner, id);
        service.linkTask(owner, id, task);
        assertEquals(task, service.getById(owner, id).getTaskId());
        service.linkTask(owner, id, null);

        FocusSession stored = repository.findById(id).orElseThrow();
        assertNull(stored.getTaskId());
        assertEquals(FocusSessionStatus.COMPLETED, stored.getStatus());
        assertNotNull(stored.getEndedAt());
        assertNull(stored.getPausedAt());
        assertThrows(IllegalStateException.class, () -> service.resume(owner, id));
    }

    @Test
    void rejectsCrossUserLinksAndScopedReads() {
        long owner = user("owner@example.com");
        long stranger = user("stranger@example.com");
        long task = task(project(stranger));
        long id = service.create(owner, null).getId();

        assertThrows(ResponseStatusException.class, () -> service.linkTask(owner, id, task));
        assertThrows(ResponseStatusException.class, () -> service.create(owner, task));
        assertThrows(ResponseStatusException.class, () -> service.getById(stranger, id));
        assertThrows(ResponseStatusException.class, () -> service.getByTask(owner, task));
        assertNull(repository.findById(id).orElseThrow().getTaskId());
    }

    @Test
    void databaseRestrictsInvalidLinksAndInvalidSessionStates() {
        long owner = user("owner@example.com");
        long id = service.create(owner, null).getId();
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("update focus_sessions set task_id=-1 where id=?", id));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("update focus_sessions set status='PAUSED' where id=?", id));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("update focus_sessions set total_paused_seconds=-1 where id=?", id));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("delete from users where id=?", owner));
    }
}
