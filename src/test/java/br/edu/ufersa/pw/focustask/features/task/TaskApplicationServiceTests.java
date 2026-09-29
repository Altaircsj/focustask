package br.edu.ufersa.pw.focustask.features.task;

import br.edu.ufersa.pw.focustask.shared.exception.EntidadeNaoEncontradaException;
import br.edu.ufersa.pw.focustask.features.project.ProjectInternalApi;
import br.edu.ufersa.pw.focustask.features.focusSession.FocusSessionInternalApi;
import org.junit.jupiter.api.Test;
import br.edu.ufersa.pw.focustask.features.task.dto.*;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskApplicationServiceTests {
    private final TaskRepository repository = mock(TaskRepository.class);
    private final ProjectInternalApi projects = mock(ProjectInternalApi.class);
    private final FocusSessionInternalApi sessions = mock(FocusSessionInternalApi.class);
    private final TaskApplicationService service = new TaskApplicationService(repository, new TaskMapperImpl(), new TaskService(projects), projects, sessions);

    @Test
    void refusesCrossUserDeletionBeforeTouchingHistory() {
        when(repository.findById(7L)).thenReturn(Optional.of(new Task(2L, "Study")));
        when(projects.pertenceAoUsuario(1L, 2L)).thenReturn(false);

        assertEquals("Task not found",
                assertThrows(EntidadeNaoEncontradaException.class, () -> service.delete(1L, 7L)).getMessage());

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
        assertEquals("Project not found",
                assertThrows(EntidadeNaoEncontradaException.class, () -> api.excluirPorProjetos(1L, List.of(2L))).getMessage());
        verifyNoInteractions(repository);
    }

    @Test
    void putAndPatchValidateDestinationBeforeMutatingAndPreserveOptionalSemantics() {
        Task task = new Task(2L, "Original");
        task.setDescription("Keep");
        task.setDueDate(java.time.LocalDate.of(2000, 1, 1));
        when(repository.findById(7L)).thenReturn(Optional.of(task));
        when(projects.pertenceAoUsuario(1L, 2L)).thenReturn(true);
        TaskPatchDTO foreign = new TaskPatchDTO(9L, "Changed", null, null, null, null);
        assertEquals("Project not found",
                assertThrows(EntidadeNaoEncontradaException.class, () -> service.patch(1L, 7L, foreign)).getMessage());
        assertEquals("Project not found",
                assertThrows(EntidadeNaoEncontradaException.class, () -> service.update(1L, 7L,
                new TaskUpdateDTO(9L, "Changed", null, TaskStatusDTO.DONE, TaskPriorityDTO.HIGH, null))).getMessage());
        assertEquals("Original", task.getTitle());
        assertEquals(2L, task.getProjectId());
        when(projects.pertenceAoUsuario(1L, 3L)).thenReturn(true);
        var patched = service.patch(1L, 7L, new TaskPatchDTO(3L, "Changed", null,
                TaskStatusDTO.DONE, TaskPriorityDTO.HIGH, null));
        assertEquals(3L, patched.projectId());
        assertEquals("Keep", patched.description());
        assertEquals(java.time.LocalDate.of(2000, 1, 1), patched.dueDate());
        var updated = service.update(1L, 7L, new TaskUpdateDTO(3L, "Changed", null,
                TaskStatusDTO.TODO, TaskPriorityDTO.LOW, null));
        assertNull(updated.description());
        assertNull(updated.dueDate());
        assertEquals(TaskStatusDTO.TODO, updated.status());
        assertEquals(updated, service.patch(1L, 7L, new TaskPatchDTO(null, null, null, null, null, null)));
    }

    @Test
    void unavailableTaskOrProjectIsNotAnEmptyCollectionOrAnAllowedUpdate() {
        assertEquals("Task not found",
                assertThrows(EntidadeNaoEncontradaException.class, () -> service.getById(1L, 99L)).getMessage());
        assertEquals("Project not found",
                assertThrows(EntidadeNaoEncontradaException.class, () -> service.getByProject(1L, 99L)).getMessage());
        when(repository.findById(7L)).thenReturn(Optional.of(new Task(2L, "Original")));
        assertEquals("Task not found",
                assertThrows(EntidadeNaoEncontradaException.class,
                () -> service.patch(1L, 7L, new TaskPatchDTO(3L, "Changed", null, null, null, null))).getMessage());
        verify(projects, never()).pertenceAoUsuario(1L, 3L);
        verify(repository, never()).save(any());
    }
}
