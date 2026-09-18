package br.edu.ufersa.pw.focustask.features.project;

import br.edu.ufersa.pw.focustask.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class ProjectMySqlTests extends MySqlIntegrationTest {
    @Autowired ProjectService service;
    @Autowired ProjectRepository repository;
    @Autowired ProjectInternalApi api;

    @Test
    void projectDeletionDeletesTasksButPreservesEverySession() {
        long owner = user("owner@example.com");
        long project = project(owner);
        long first = task(project);
        long second = task(project);
        long firstSession = session(owner, first);
        long secondSession = session(owner, second);
        long otherProject = project(owner);
        long otherTask = task(otherProject);
        long otherSession = session(owner, otherTask);

        service.delete(owner, project);

        assertFalse(repository.existsById(project));
        assertEquals(0, jdbc.queryForObject("select count(*) from tasks where project_id=?", Integer.class, project));
        assertNull(jdbc.queryForObject("select task_id from focus_sessions where id=?", Long.class, firstSession));
        assertNull(jdbc.queryForObject("select task_id from focus_sessions where id=?", Long.class, secondSession));
        assertEquals(otherTask, jdbc.queryForObject("select task_id from focus_sessions where id=?", Long.class, otherSession));
        assertTrue(repository.existsById(otherProject));
    }

    @Test
    void facadeUsesServerOwnershipAndReturnsPublicDTOs() {
        long owner = user("owner@example.com");
        long stranger = user("stranger@example.com");
        ProjectDTO result = api.criarProjeto(owner, new ProjectDTO(999L, stranger, "Created", null));

        assertNotEquals(999L, result.id());
        assertEquals(owner, result.userId());
        assertEquals(1, api.listarPorUsuario(owner).size());
        assertTrue(api.listarPorUsuario(stranger).isEmpty());
        assertThrows(ResponseStatusException.class, () -> service.delete(stranger, result.id()));
    }

    @Test
    void emptyProjectCanBeDeletedWithoutAnEmptyInQuery() {
        long owner = user("owner@example.com");
        long project = project(owner);
        service.delete(owner, project);
        assertFalse(repository.existsById(project));
    }
}
