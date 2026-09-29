package br.edu.ufersa.pw.focustask.features.task;

import br.edu.ufersa.pw.focustask.features.focusSession.FocusSessionInternalApi;
import br.edu.ufersa.pw.focustask.features.project.ProjectInternalApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@Import(TaskService.class)
class TaskControllerTests {
    @Autowired MockMvc mvc;
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

    @Test
    void createsTaskFromUrlProjectAndReturnsCanonicalLocation() throws Exception {
        existingProject();

        mvc.perform(post("/api/v1/projects/42/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Study", "description":"Read the examples", "dueDate":"2026-10-05"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/users/7/tasks/123"))
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
                .andExpect(header().string("Location", "http://localhost/focustask/api/v1/users/7/tasks/123"))
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
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        mvc.perform(post("/api/v1/projects/42/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Study\"}"))
                .andExpect(status().isNotFound())
                .andExpect(header().doesNotExist("Location"));
        verifyNoInteractions(repository, sessions);
    }

    @Test
    void oldRouteNoLongerAcceptsCreation() throws Exception {
        mvc.perform(post("/api/v1/users/7/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Study\"}"))
                .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(repository, projects, sessions);
    }
}
