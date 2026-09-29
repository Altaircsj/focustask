package br.edu.ufersa.pw.focustask.features.project;

import br.edu.ufersa.pw.focustask.shared.exception.EntidadeNaoEncontradaException;
import br.edu.ufersa.pw.focustask.features.task.TaskInternalApi;
import br.edu.ufersa.pw.focustask.features.focusSession.FocusSessionInternalApi;
import org.junit.jupiter.api.Test;
import br.edu.ufersa.pw.focustask.features.project.dto.*;
import br.edu.ufersa.pw.focustask.features.user.UserInternalApi;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectApplicationServiceTests {
    @Test
    void deletionDetachesSessionsBeforeDeletingChildrenAndParent() {
        ProjectRepository repository = mock(ProjectRepository.class);
        UserInternalApi users = mock(UserInternalApi.class);
        TaskInternalApi tasks = mock(TaskInternalApi.class);
        FocusSessionInternalApi sessions = mock(FocusSessionInternalApi.class);
        ProjectApplicationService service = new ProjectApplicationService(repository, new ProjectMapperImpl(), users, tasks, sessions);
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
        UserInternalApi users = mock(UserInternalApi.class);
        TaskInternalApi tasks = mock(TaskInternalApi.class);
        FocusSessionInternalApi sessions = mock(FocusSessionInternalApi.class);
        ProjectApplicationService service = new ProjectApplicationService(repository, new ProjectMapperImpl(), users, tasks, sessions);
        when(repository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.empty());

        assertEquals("Project not found",
                assertThrows(EntidadeNaoEncontradaException.class, () -> service.delete(1L, 2L)).getMessage());
        verifyNoInteractions(tasks, sessions);
        verify(repository, never()).deleteById(any());
    }

    @Test
    void validatesCollectionParentAndMapsCreationWithServerOwner() {
        ProjectRepository repository = mock(ProjectRepository.class);
        UserInternalApi users = mock(UserInternalApi.class);
        ProjectApplicationService service = new ProjectApplicationService(repository, new ProjectMapperImpl(),
                users, mock(TaskInternalApi.class), mock(FocusSessionInternalApi.class));
        assertEquals("User not found",
                assertThrows(EntidadeNaoEncontradaException.class,
                () -> service.getAll(1L)).getMessage());
        assertEquals("User not found",
                assertThrows(EntidadeNaoEncontradaException.class,
                () -> service.create(1L, new ProjectCreateDTO("Project", null))).getMessage());
        verifyNoInteractions(repository);
        when(users.existePorId(1L)).thenReturn(true);
        when(repository.findAllByUserId(1L)).thenReturn(List.of());
        assertTrue(service.getAll(1L).isEmpty());
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        assertEquals(1L, service.create(1L, new ProjectCreateDTO("Project", "Description")).userId());
    }

    @Test
    void scopedUpdatesPreserveOwnerAndIdAndDistinguishPutFromPatch() {
        ProjectRepository repository = mock(ProjectRepository.class);
        ProjectApplicationService service = new ProjectApplicationService(repository, new ProjectMapperImpl(),
                mock(UserInternalApi.class), mock(TaskInternalApi.class), mock(FocusSessionInternalApi.class));
        Project project = new Project(1L, "Original", "Keep");
        org.springframework.test.util.ReflectionTestUtils.setField(project, "id", 2L);
        when(repository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(project));
        var patched = service.patch(1L, 2L, new ProjectPatchDTO("Updated", null));
        assertEquals("Keep", patched.description());
        var updated = service.update(1L, 2L, new ProjectUpdateDTO("Updated", null));
        assertNull(updated.description());
        assertEquals(1L, updated.userId());
        assertEquals(2L, updated.id());
        assertEquals(updated, service.getById(1L, 2L));
        assertEquals("Project not found",
                assertThrows(EntidadeNaoEncontradaException.class,
                () -> service.patch(9L, 2L, new ProjectPatchDTO("Foreign", null))).getMessage());
        assertEquals("Updated", project.getName());
    }
}
