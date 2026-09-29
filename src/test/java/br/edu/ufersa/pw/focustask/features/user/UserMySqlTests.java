package br.edu.ufersa.pw.focustask.features.user;

import br.edu.ufersa.pw.focustask.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import br.edu.ufersa.pw.focustask.features.user.dto.*;
import static org.junit.jupiter.api.Assertions.*;

class UserMySqlTests extends MySqlIntegrationTest {
    @Autowired UserApplicationService service;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean UserRepository repository;

    @Test
    void deletingUserRemovesAllOwnedDataIncludingStandaloneSessions() {
        long owner = user("owner@example.com");
        long ownedProject = project(owner);
        long ownedTask = task(ownedProject);
        session(owner, ownedTask);
        session(owner, null);
        long stranger = user("stranger@example.com");
        long otherProject = project(stranger);
        long otherTask = task(otherProject);
        long otherSession = session(stranger, otherTask);

        service.delete(owner);

        assertFalse(repository.existsById(owner));
        assertEquals(0, jdbc.queryForObject("select count(*) from projects where user_id=?", Integer.class, owner));
        assertEquals(0, jdbc.queryForObject("select count(*) from tasks where project_id=?", Integer.class, ownedProject));
        assertEquals(0, jdbc.queryForObject("select count(*) from focus_sessions where user_id=?", Integer.class, owner));
        assertTrue(repository.existsById(stranger));
        assertEquals(otherTask, jdbc.queryForObject("select task_id from focus_sessions where id=?", Long.class, otherSession));
    }

    @Test
    void normalizesEmailAndEnforcesUniquenessAndRequiredFields() {
        UserResponseDTO result = service.create(new UserCreateDTO("Student", "  Student@Example.COM  "));
        assertEquals("student@example.com", result.email());
        assertThrows(EmailAlreadyExistsException.class, () -> service.create(new UserCreateDTO("Other", "STUDENT@example.com")));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("insert into users(name,email) values ('Other','student@example.com')"));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("insert into users(name,email) values (null,'new@example.com')"));
    }

    @Test
    void deletesUserWithNoProjects() {
        long owner = user("owner@example.com");
        session(owner, null);
        service.delete(owner);
        assertFalse(repository.existsById(owner));
        assertEquals(0, jdbc.queryForObject("select count(*) from focus_sessions", Integer.class));
    }

    @Test
    void coordinatorRollsBackAllDeletionStepsWhenFinalFlushFails() {
        long owner = user("rollback@example.com");
        long project = project(owner);
        long task = task(project);
        long session = session(owner, task);
        var failure = new org.springframework.dao.DataAccessResourceFailureException("Simulated final flush failure");
        org.mockito.Mockito.doThrow(failure).when(repository).flush();
        assertSame(failure, assertThrows(org.springframework.dao.DataAccessResourceFailureException.class,
                () -> service.delete(owner)));
        assertEquals(1, jdbc.queryForObject("select count(*) from users where id=?", Integer.class, owner));
        assertEquals(1, jdbc.queryForObject("select count(*) from projects where id=?", Integer.class, project));
        assertEquals(1, jdbc.queryForObject("select count(*) from tasks where id=?", Integer.class, task));
        assertEquals(task, jdbc.queryForObject("select task_id from focus_sessions where id=?", Long.class, session));
    }

    @Test
    void putAndPatchCommitAndDuplicateEmailDoesNotPartiallyChangeTheUser() {
        long owner = user("original@example.com");
        user("taken@example.com");
        assertThrows(EmailAlreadyExistsException.class,
                () -> service.update(owner, new UserUpdateDTO("Rejected", "TAKEN@example.com")));
        assertEquals("Student", repository.findById(owner).orElseThrow().getName());
        service.update(owner, new UserUpdateDTO("Changed", "NEW@example.com"));
        assertEquals("new@example.com", repository.findById(owner).orElseThrow().getEmail());
        service.patch(owner, new UserPatchDTO("Patched", null));
        assertEquals("Patched", repository.findById(owner).orElseThrow().getName());
        assertEquals("new@example.com", repository.findById(owner).orElseThrow().getEmail());
        assertThrows(EmailAlreadyExistsException.class,
                () -> service.patch(owner, new UserPatchDTO("Rejected", "taken@example.com")));
        assertEquals("Patched", repository.findById(owner).orElseThrow().getName());
    }
}
