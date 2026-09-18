package br.edu.ufersa.pw.focustask.features.task;

import br.edu.ufersa.pw.focustask.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TaskMySqlTests extends MySqlIntegrationTest {
    @Autowired TaskService service;
    @Autowired TaskRepository repository;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void taskDeletionPreservesLinkedAndStandaloneHistory() {
        long owner = user("owner@example.com");
        long project = project(owner);
        long task = task(project);
        long linked = session(owner, task);
        long standalone = session(owner, null);
        var before = jdbc.queryForMap("select * from focus_sessions where id=?", linked);

        service.delete(owner, task);

        assertFalse(repository.existsById(task));
        var after = jdbc.queryForMap("select * from focus_sessions where id=?", linked);
        before.put("task_id", null);
        assertEquals(before, after);
        assertEquals(1, jdbc.queryForObject("select count(*) from focus_sessions where id=?", Integer.class, standalone));
    }

    @Test
    void failureInOuterTransactionRollsBackDeletionAndDetachment() {
        long owner = user("owner@example.com");
        long task = task(project(owner));
        long session = session(owner, task);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            service.delete(owner, task);
            throw new IllegalStateException("Simulated later failure");
        }));

        assertTrue(repository.existsById(task));
        assertEquals(task, jdbc.queryForObject("select task_id from focus_sessions where id=?", Long.class, session));
    }

    @Test
    void scopesQueriesAndRejectsAnotherUsersProjectAndTask() {
        long owner = user("owner@example.com");
        long stranger = user("stranger@example.com");
        long project = project(owner);
        long task = task(project);
        project(stranger);

        assertEquals(List.of(task), service.getAll(owner).stream().map(Task::getId).toList());
        assertTrue(service.getAll(stranger).isEmpty());
        assertThrows(ResponseStatusException.class, () -> service.getById(stranger, task));
        assertThrows(ResponseStatusException.class, () -> service.delete(stranger, task));
        assertThrows(ResponseStatusException.class, () -> service.create(stranger, project, "Task", null, null));
        assertTrue(repository.existsById(task));
    }

    @Test
    void databaseEnforcesForeignKeysAndEnumsAndAllowsOptionalValues() {
        long owner = user("owner@example.com");
        long project = project(owner);
        Task task = service.create(owner, project, "Task", null, null);

        assertNull(task.getDescription());
        assertNull(task.getDueDate());
        assertEquals("TODO", jdbc.queryForObject("select status from tasks where id=?", String.class, task.getId()));
        assertEquals("MEDIUM", jdbc.queryForObject("select priority from tasks where id=?", String.class, task.getId()));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("insert into tasks(project_id,title) values (-1,'Invalid')"));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("update tasks set status='UNKNOWN' where id=?", task.getId()));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("delete from projects where id=?", project));
    }

    @Test
    void projectChangeIsAllowedOnlyWithinTheSameUser() {
        long owner = user("owner@example.com");
        long source = project(owner);
        long destination = project(owner);
        long strangerProject = project(user("stranger@example.com"));
        long task = task(source);

        service.update(owner, task, destination, "Moved", null, TaskStatus.TODO, TaskPriority.MEDIUM, null);
        assertEquals(destination, repository.findById(task).orElseThrow().getProjectId());
        assertThrows(ResponseStatusException.class, () -> service.update(owner, task, strangerProject,
                "Invalid", null, TaskStatus.TODO, TaskPriority.MEDIUM, null));
        assertEquals(destination, repository.findById(task).orElseThrow().getProjectId());
    }
}
