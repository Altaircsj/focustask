package br.edu.ufersa.pw.focustask.features.task;

import br.edu.ufersa.pw.focustask.shared.exception.EntidadeNaoEncontradaException;
import br.edu.ufersa.pw.focustask.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import br.edu.ufersa.pw.focustask.features.task.dto.*;
import br.edu.ufersa.pw.focustask.shared.exception.GlobalExceptionHandler;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TaskMySqlTests extends MySqlIntegrationTest {
    @Autowired TaskApplicationService service;
    @Autowired TaskController controller;
    @Autowired org.springframework.web.context.WebApplicationContext context;
    private org.springframework.test.web.servlet.MockMvc mvc(long owner) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .defaultRequest(post("/").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(
                        br.edu.ufersa.pw.focustask.AuthenticatedMvcTest.principal(owner)))).build();
    }
    @Autowired GlobalExceptionHandler handler;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean TaskRepository repository;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void postPersistsFreshTaskUnderUrlProjectAndKeepsExistingTask() throws Exception {
        long owner = user("owner@example.com");
        long urlProject = project(owner);
        long otherProject = project(user("other@example.com"));
        long existingTask = task(otherProject);

        var response = mvc(owner)
                .perform(post("/api/v1/projects/{projectId}/tasks", urlProject)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":%d,"projectId":%d,"title":"New task",
                                 "description":"Study examples","dueDate":"2026-10-05"}
                                """.formatted(existingTask, otherProject)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value(urlProject))
                .andReturn().getResponse();

        Task created = repository.findAllByProjectId(urlProject).getFirst();
        assertNotEquals(existingTask, created.getId());
        assertEquals("New task", created.getTitle());
        assertEquals("Study examples", created.getDescription());
        assertEquals(java.time.LocalDate.of(2026, 10, 5), created.getDueDate());
        assertEquals(TaskStatus.TODO, created.getStatus());
        assertEquals(TaskPriority.MEDIUM, created.getPriority());
        assertEquals("http://localhost/api/v1/tasks/" + created.getId(),
                response.getHeader("Location"));
        Task original = repository.findById(existingTask).orElseThrow();
        assertEquals(otherProject, original.getProjectId());
        assertEquals("Task", original.getTitle());
        assertEquals(2, repository.count());
    }

    @Test
    void postRejectsMissingProjectWithoutWriting() throws Exception {
        mvc(user("owner@example.com"))
                .perform(post("/api/v1/projects/-1/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Study\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Project not found"));

        assertEquals(0, repository.count());
    }

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

        assertEquals(List.of(task), service.getAll(owner).stream().map(TaskResponseDTO::id).toList());
        assertTrue(service.getAll(stranger).isEmpty());
        assertEquals("Task not found",
                assertThrows(EntidadeNaoEncontradaException.class, () -> service.getById(stranger, task)).getMessage());
        assertEquals("Task not found",
                assertThrows(EntidadeNaoEncontradaException.class, () -> service.delete(stranger, task)).getMessage());
        assertEquals("Project not found",
                assertThrows(EntidadeNaoEncontradaException.class, () -> service.getByProject(stranger, project)).getMessage());
        assertTrue(repository.existsById(task));
    }

    @Test
    void databaseEnforcesForeignKeysAndEnumsAndAllowsOptionalValues() {
        long owner = user("owner@example.com");
        long project = project(owner);
        TaskResponseDTO task = service.create(owner, project, new TaskCreateDTO("Task", null, null));

        assertNull(task.description());
        assertNull(task.dueDate());
        assertEquals("TODO", jdbc.queryForObject("select status from tasks where id=?", String.class, task.id()));
        assertEquals("MEDIUM", jdbc.queryForObject("select priority from tasks where id=?", String.class, task.id()));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("insert into tasks(project_id,title) values (-1,'Invalid')"));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("update tasks set status='UNKNOWN' where id=?", task.id()));
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

        service.update(owner, task, new TaskUpdateDTO(destination, "Moved", null, TaskStatusDTO.TODO, TaskPriorityDTO.MEDIUM, null));
        assertEquals(destination, repository.findById(task).orElseThrow().getProjectId());
        assertEquals("Project not found",
                assertThrows(EntidadeNaoEncontradaException.class, () -> service.update(owner, task, new TaskUpdateDTO(strangerProject,
                "Invalid", null, TaskStatusDTO.TODO, TaskPriorityDTO.MEDIUM, null))).getMessage());
        assertEquals(destination, repository.findById(task).orElseThrow().getProjectId());
    }

    @Test
    void coordinatorRollsBackAllDeletionStepsWhenFinalFlushFails() {
        long owner = user("rollback@example.com");
        long project = project(owner);
        long task = task(project);
        long session = session(owner, task);
        org.mockito.Mockito.doThrow(new IllegalStateException("Simulated final flush failure"))
                .when(repository).flush();
        assertThrows(IllegalStateException.class, () -> service.delete(owner, task));
        assertEquals(1, jdbc.queryForObject("select count(*) from users where id=?", Integer.class, owner));
        assertEquals(1, jdbc.queryForObject("select count(*) from projects where id=?", Integer.class, project));
        assertEquals(1, jdbc.queryForObject("select count(*) from tasks where id=?", Integer.class, task));
        assertEquals(task, jdbc.queryForObject("select task_id from focus_sessions where id=?", Long.class, session));
    }
}
