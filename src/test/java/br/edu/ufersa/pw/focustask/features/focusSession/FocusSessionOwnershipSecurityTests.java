package br.edu.ufersa.pw.focustask.features.focusSession;

import br.edu.ufersa.pw.focustask.AuthenticatedMvcTest;
import br.edu.ufersa.pw.focustask.features.user.UserInternalApi;
import br.edu.ufersa.pw.focustask.features.task.TaskInternalApi;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import java.time.Clock;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FocusSessionController.class)
@Import({FocusSessionApplicationService.class, FocusSessionService.class, FocusSessionMapperImpl.class, FocusSessionConfiguration.class})
class FocusSessionOwnershipSecurityTests extends AuthenticatedMvcTest {
    @MockitoBean FocusSessionRepository repository;
    @MockitoBean UserInternalApi users;
    @MockitoBean TaskInternalApi tasks;

    @Test void adminCannotReadChangeDeleteOrAttachForeignResources() throws Exception {
        when(users.existePorId(7L)).thenReturn(true);
        mvc.perform(get("/api/v1/focus-sessions/88")).andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/focus-sessions/88").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"COMPLETED\"}")).andExpect(status().isNotFound());
        mvc.perform(patch("/api/v1/focus-sessions/88").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"COMPLETED\"}")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/focus-sessions/88")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/tasks/99/focus-sessions")).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/tasks/99/focus-sessions")).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/focus-sessions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"taskId\":99}")).andExpect(status().isNotFound());
        verify(repository).findByIdAndUserId(88L, 7L);
        verify(repository, times(3)).findForUpdate(88L, 7L);
        verify(repository, never()).save(any());
        verify(repository, never()).delete(any(FocusSession.class));
    }

    @Test void rejectsForeignTaskBeforeChangingOwnedSessionStatusOrLink() throws Exception {
        FocusSession owned = new FocusSession(7L, Clock.systemUTC());
        var started = owned.getStartedAt();
        when(repository.findForUpdate(88L, 7L)).thenReturn(Optional.of(owned));
        mvc.perform(patch("/api/v1/focus-sessions/88").contentType(MediaType.APPLICATION_JSON)
                .content("{\"taskId\":99,\"status\":\"COMPLETED\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value("Task not found"));
        assertEquals(FocusSessionStatus.RUNNING, owned.getStatus());
        assertNull(owned.getTaskId());
        assertNull(owned.getEndedAt());
        assertEquals(started, owned.getStartedAt());
    }
}
