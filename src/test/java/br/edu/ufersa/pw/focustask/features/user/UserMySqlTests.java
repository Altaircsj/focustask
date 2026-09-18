package br.edu.ufersa.pw.focustask.features.user;

import br.edu.ufersa.pw.focustask.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class UserMySqlTests extends MySqlIntegrationTest {
    @Autowired UserService service;
    @Autowired UserRepository repository;

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
        UserDTO result = service.create("Student", "  Student@Example.COM  ");
        assertEquals("student@example.com", result.email());
        assertThrows(ResponseStatusException.class, () -> service.create("Other", "STUDENT@example.com"));
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
}
