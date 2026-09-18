package br.edu.ufersa.pw.focustask.features.task;

import br.edu.ufersa.pw.focustask.features.project.ProjectInternalApi;
import br.edu.ufersa.pw.focustask.features.focusSession.FocusSessionInternalApi;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskServiceTests {
    private final TaskRepository repository = mock(TaskRepository.class);
    private final ProjectInternalApi projects = mock(ProjectInternalApi.class);
    private final FocusSessionInternalApi sessions = mock(FocusSessionInternalApi.class);
    private final TaskService service = new TaskService(repository, projects, sessions);

    @Test
    void refusesCrossUserDeletionBeforeTouchingHistory() {
        when(repository.findById(7L)).thenReturn(Optional.of(new Task(2L, "Study")));
        when(projects.pertenceAoUsuario(1L, 2L)).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> service.delete(1L, 7L));

        verifyNoInteractions(sessions);
        verify(repository, never()).deleteById(any());
    }

    @Test
    void deletesOnlyAfterSessionsHaveBeenDetached() {
        when(repository.findById(7L)).thenReturn(Optional.of(new Task(2L, "Study")));
        when(projects.pertenceAoUsuario(1L, 2L)).thenReturn(true);

        service.delete(1L, 7L);

        var order = inOrder(sessions, repository);
        order.verify(sessions).desvincularDeTarefas(1L, List.of(7L));
        order.verify(repository).deleteById(7L);
    }

    @Test
    void detachmentFailurePreventsTaskDeletion() {
        when(repository.findById(7L)).thenReturn(Optional.of(new Task(2L, "Study")));
        when(projects.pertenceAoUsuario(1L, 2L)).thenReturn(true);
        doThrow(new IllegalStateException("Failed")).when(sessions).desvincularDeTarefas(1L, List.of(7L));

        assertThrows(IllegalStateException.class, () -> service.delete(1L, 7L));
        verify(repository, never()).deleteById(any());
    }

    @Test
    void userWithoutProjectsDoesNotGenerateAnEmptyInQuery() {
        when(projects.listarPorUsuario(1L)).thenReturn(List.of());
        assertTrue(service.getAll(1L).isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    void internalBulkApiRejectsForeignProjectsBeforeWriting() {
        TaskInternalApi api = new TaskInternalApiImpl(repository, projects);
        when(projects.pertenceAoUsuario(1L, 2L)).thenReturn(false);
        assertThrows(ResponseStatusException.class, () -> api.excluirPorProjetos(1L, List.of(2L)));
        verifyNoInteractions(repository);
    }
}
