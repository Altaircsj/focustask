package br.edu.ufersa.pw.focustask.features.task;

import br.edu.ufersa.pw.focustask.shared.exception.EntidadeNaoEncontradaException;
import br.edu.ufersa.pw.focustask.features.focusSession.FocusSessionInternalApi;
import br.edu.ufersa.pw.focustask.features.project.ProjectInternalApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import java.util.Optional;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@Import({TaskApplicationService.class, TaskService.class, TaskMapperImpl.class})
class TaskControllerTests extends br.edu.ufersa.pw.focustask.AuthenticatedMvcTest {
    @MockitoBean TaskRepository repository;
    @MockitoBean ProjectInternalApi projects;
    @MockitoBean FocusSessionInternalApi sessions;

    private void existingProject() {
        when(projects.obterUsuarioIdPorProjeto(42L)).thenReturn(7L);
        when(projects.pertenceAoUsuario(7L, 42L)).thenReturn(true);
        when(repository.save(any(Task.class))).thenAnswer(call -> {
            Task task = call.getArgument(0);
            assertNull(task.getId(), "Creation must persist a fresh entity");
            ReflectionTestUtils.setField(task, "id", 123L);
            return task;
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"title\":null}", "{\"title\":\" \"}"})
    void invalidCreateBodyReturns400BeforeLookingUpProject(String body) throws Exception {
        mvc.perform(post("/api/v1/projects/42/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.title").isArray());
        verifyNoInteractions(repository, projects, sessions);
    }

    @Test
    void acceptsPastDueDateAndRejectsOversizedTitle() throws Exception {
        mvc.perform(post("/api/v1/projects/42/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + "T".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(repository, projects, sessions);
        existingProject();
        mvc.perform(post("/api/v1/projects/42/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Study\",\"dueDate\":\"2000-01-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dueDate").value("2000-01-01"));
    }

    @Test
    void updateAndPatchExecuteAndPreserveOptionalValues() throws Exception {
        String path = "/api/v1/tasks/123";
        Task stored = new Task(42L, "Original");
        stored.setDescription("Keep");
        when(repository.findById(123L)).thenReturn(Optional.of(stored));
        when(projects.pertenceAoUsuario(7L, 42L)).thenReturn(true);
        mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content("""
                        {"projectId":42,"title":"Study","status":"TODO","priority":"MEDIUM"}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Study"));
        mvc.perform(patch(path).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Study"));
        mvc.perform(patch(path).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":null,\"description\":null,\"dueDate\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Study"));
        mvc.perform(patch(path).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\" \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch(path).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"UNKNOWN\"}"))
                .andExpect(status().isBadRequest());
        assertNull(stored.getDescription(), "PUT clears omitted optional fields");
        verifyNoInteractions(sessions);
    }

    @Test
    void createsTaskFromUrlProjectAndReturnsCanonicalLocation() throws Exception {
        existingProject();

        mvc.perform(post("/api/v1/projects/42/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Study", "description":"Read the examples", "dueDate":"2026-10-05"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/tasks/123"))
                .andExpect(jsonPath("$.id").value(123))
                .andExpect(jsonPath("$.projectId").value(42))
                .andExpect(jsonPath("$.title").value("Study"))
                .andExpect(jsonPath("$.description").value("Read the examples"))
                .andExpect(jsonPath("$.dueDate").value("2026-10-05"));
        verify(repository).save(argThat(task -> task.getProjectId().equals(42L)));
        verifyNoInteractions(sessions);
    }

    @Test
    void ignoresBodyIdentityAndPreservesCreationDefaultsAndContextPath() throws Exception {
        existingProject();

        mvc.perform(post("/focustask/api/v1/projects/42/tasks").contextPath("/focustask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":999,"projectId":888,"title":"Study","status":"DONE","priority":"HIGH"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/focustask/api/v1/tasks/123"))
                .andExpect(jsonPath("$.id").value(123))
                .andExpect(jsonPath("$.projectId").value(42))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.description").isEmpty())
                .andExpect(jsonPath("$.dueDate").isEmpty());
        verify(projects, never()).obterUsuarioIdPorProjeto(888L);
        verify(repository, never()).findById(anyLong());
    }

    @Test
    void missingProjectReturns404WithoutSaving() throws Exception {
        when(projects.obterUsuarioIdPorProjeto(42L))
                .thenThrow(new EntidadeNaoEncontradaException("Project not found"));

        mvc.perform(post("/focustask/api/v1/projects/42/tasks").contextPath("/focustask")
                        .queryParam("trace", "true").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Study\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Project not found"))
                .andExpect(jsonPath("$.instance").value("/focustask/api/v1/projects/42/tasks"));
        verifyNoInteractions(repository, sessions);
    }

    @Test
    void oldRouteNoLongerAcceptsCreation() throws Exception {
        mvc.perform(post("/api/v1/users/7/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Study\"}"))
                .andExpect(status().isNotFound());
        verifyNoInteractions(repository, projects, sessions);
    }

    @Test
    void readsAllRegisteredCollectionsAndIndividualTaskThenDeletes() throws Exception {
        Task task = new Task(42L, "Study");
        ReflectionTestUtils.setField(task, "id", 123L);
        when(projects.pertenceAoUsuario(7L, 42L)).thenReturn(true);
        when(projects.listarPorUsuario(7L)).thenReturn(List.of(
                new br.edu.ufersa.pw.focustask.features.project.ProjectDTO(42L, 7L, "Project", null)));
        when(repository.findById(123L)).thenReturn(Optional.of(task));
        when(repository.findAllByProjectIdIn(List.of(42L))).thenReturn(List.of(task));
        when(repository.findAllByProjectId(42L)).thenReturn(List.of(task));
        mvc.perform(get("/api/v1/tasks")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(123));
        mvc.perform(get("/api/v1/projects/42/tasks")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(123));
        mvc.perform(get("/api/v1/tasks/123")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(123));
        mvc.perform(delete("/api/v1/tasks/123")).andExpect(status().isNoContent());
        verify(sessions).desvincularDeTarefas(7L, List.of(123L));
        verify(repository).deleteById(123L);
    }
    @Test
    void authenticatedAdminCannotCreateReadChangeOrDeleteAnotherUsersTask() throws Exception {
        Task foreign = new Task(88L, "Private");
        when(repository.findById(123L)).thenReturn(Optional.of(foreign));
        mvc.perform(post("/api/v1/projects/88/tasks").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Attack\"}")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Project not found"));
        mvc.perform(get("/api/v1/tasks/123")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/projects/88/tasks")).andExpect(status().isNotFound());
        mvc.perform(patch("/api/v1/tasks/123").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Attack\"}")).andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/tasks/123").contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectId\":42,\"title\":\"Attack\",\"status\":\"TODO\",\"priority\":\"MEDIUM\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/tasks/123")).andExpect(status().isNotFound());
        assertEquals("Private", foreign.getTitle());
        verify(repository, never()).save(any());
        verify(repository, never()).deleteById(any());
        verifyNoInteractions(sessions);
    }

    @Test
    void rejectsMovingOwnedTaskToForeignProjectWithoutMutation() throws Exception {
        Task own = new Task(42L, "Owned");
        when(repository.findById(123L)).thenReturn(Optional.of(own));
        when(projects.pertenceAoUsuario(7L, 42L)).thenReturn(true);
        mvc.perform(patch("/api/v1/tasks/123").contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectId\":88,\"title\":\"Attack\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value("Project not found"));
        assertEquals(42L, own.getProjectId());
        assertEquals("Owned", own.getTitle());
    }
}
