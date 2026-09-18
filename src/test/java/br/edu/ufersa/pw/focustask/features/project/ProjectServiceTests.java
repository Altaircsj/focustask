package br.edu.ufersa.pw.focustask.features.project;

import br.edu.ufersa.pw.focustask.features.task.TaskInternalApi;
import br.edu.ufersa.pw.focustask.features.focusSession.FocusSessionInternalApi;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectServiceTests {
    @Test
    void deletionDetachesSessionsBeforeDeletingChildrenAndParent() {
        ProjectRepository repository = mock(ProjectRepository.class);
        ProjectInternalApi projects = mock(ProjectInternalApi.class);
        TaskInternalApi tasks = mock(TaskInternalApi.class);
        FocusSessionInternalApi sessions = mock(FocusSessionInternalApi.class);
        ProjectService service = new ProjectService(repository, projects, tasks, sessions);
        when(repository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(new Project(1L, "Project", null)));
        when(tasks.listarIdsPorProjetos(1L, List.of(2L))).thenReturn(List.of(7L, 8L));

        service.delete(1L, 2L);

        var order = inOrder(sessions, tasks, repository);
        order.verify(sessions).desvincularDeTarefas(1L, List.of(7L, 8L));
        order.verify(tasks).excluirPorProjetos(1L, List.of(2L));
        order.verify(repository).deleteById(2L);
    }

    @Test
    void missingOrForeignProjectDoesNotTriggerDeletion() {
        ProjectRepository repository = mock(ProjectRepository.class);
        ProjectInternalApi projects = mock(ProjectInternalApi.class);
        TaskInternalApi tasks = mock(TaskInternalApi.class);
        FocusSessionInternalApi sessions = mock(FocusSessionInternalApi.class);
        ProjectService service = new ProjectService(repository, projects, tasks, sessions);
        when(repository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.delete(1L, 2L));
        verifyNoInteractions(tasks, sessions);
        verify(repository, never()).deleteById(any());
    }
}
