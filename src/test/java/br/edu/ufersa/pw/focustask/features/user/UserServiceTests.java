package br.edu.ufersa.pw.focustask.features.user;

import br.edu.ufersa.pw.focustask.features.project.ProjectDTO;
import br.edu.ufersa.pw.focustask.features.project.ProjectInternalApi;
import br.edu.ufersa.pw.focustask.features.task.TaskInternalApi;
import br.edu.ufersa.pw.focustask.features.focusSession.FocusSessionInternalApi;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTests {
    @Test
    void deletionRemovesSessionsBeforeTasksProjectsAndUser() {
        UserRepository repository = mock(UserRepository.class);
        ProjectInternalApi projects = mock(ProjectInternalApi.class);
        TaskInternalApi tasks = mock(TaskInternalApi.class);
        FocusSessionInternalApi sessions = mock(FocusSessionInternalApi.class);
        UserService service = new UserService(repository, projects, tasks, sessions);
        when(repository.findById(1L)).thenReturn(Optional.of(new User("Student", "student@example.com")));
        when(projects.listarPorUsuario(1L)).thenReturn(List.of(new ProjectDTO(2L, 1L, "Project", null)));

        service.delete(1L);

        var order = inOrder(sessions, tasks, projects, repository);
        order.verify(sessions).excluirPorUsuario(1L);
        order.verify(tasks).excluirPorProjetos(1L, List.of(2L));
        order.verify(projects).excluirPorUsuario(1L);
        order.verify(repository).deleteById(1L);
    }

    @Test
    void checksNormalizedEmailBeforeSaving() {
        UserRepository repository = mock(UserRepository.class);
        UserService service = new UserService(repository, mock(ProjectInternalApi.class),
                mock(TaskInternalApi.class), mock(FocusSessionInternalApi.class));
        when(repository.existsByEmail("student@example.com")).thenReturn(true);

        assertThrows(ResponseStatusException.class,
                () -> service.create("Student", "  STUDENT@EXAMPLE.COM  "));
        verify(repository, never()).save(any());
    }
}
