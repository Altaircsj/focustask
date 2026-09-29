package br.edu.ufersa.pw.focustask.features.project;

import br.edu.ufersa.pw.focustask.AuthenticatedMvcTest;
import br.edu.ufersa.pw.focustask.features.user.UserInternalApi;
import br.edu.ufersa.pw.focustask.features.task.TaskInternalApi;
import br.edu.ufersa.pw.focustask.features.focusSession.FocusSessionInternalApi;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectController.class)
@Import({ProjectApplicationService.class, ProjectMapperImpl.class})
class ProjectOwnershipSecurityTests extends AuthenticatedMvcTest {
    @MockitoBean ProjectRepository repository;
    @MockitoBean UserInternalApi users;
    @MockitoBean TaskInternalApi tasks;
    @MockitoBean FocusSessionInternalApi sessions;

    @Test void adminCannotReadUpdatePatchOrDeleteForeignProject() throws Exception {
        mvc.perform(get("/api/v1/projects/88")).andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/projects/88").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Attack\"}")).andExpect(status().isNotFound());
        mvc.perform(patch("/api/v1/projects/88").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Attack\"}")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/projects/88")).andExpect(status().isNotFound());
        verify(repository, times(4)).findByIdAndUserId(88L, 7L);
        verify(repository, never()).save(any());
        verify(repository, never()).deleteById(any());
        verifyNoInteractions(tasks, sessions);
    }

    @Test void collectionAndCreationUseAuthenticatedOwnerEvenForAdmin() throws Exception {
        when(users.existePorId(7L)).thenReturn(true);
        when(repository.findAllByUserId(7L)).thenReturn(List.of(new Project(7L, "Owned", null)));
        when(repository.save(any())).thenAnswer(c -> {
            Project saved = c.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(saved, "id", 12L);
            return saved;
        });
        mvc.perform(get("/api/v1/projects")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(7));
        mvc.perform(post("/api/v1/projects").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.userId").value(7));
        verify(repository).save(argThat(project -> project.getUserId().equals(7L)));
        verify(repository, never()).findAll();
    }
}
