package br.edu.ufersa.pw.focustask.features.user;

import br.edu.ufersa.pw.focustask.shared.exception.EntidadeNaoEncontradaException;
import br.edu.ufersa.pw.focustask.features.project.ProjectDTO;
import br.edu.ufersa.pw.focustask.features.project.ProjectInternalApi;
import br.edu.ufersa.pw.focustask.features.task.TaskInternalApi;
import br.edu.ufersa.pw.focustask.features.focusSession.FocusSessionInternalApi;
import org.junit.jupiter.api.Test;
import br.edu.ufersa.pw.focustask.features.user.dto.*;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserApplicationServiceTests {
    @Test
    void deletionRemovesSessionsBeforeTasksProjectsAndUser() {
        UserRepository repository = mock(UserRepository.class);
        ProjectInternalApi projects = mock(ProjectInternalApi.class);
        TaskInternalApi tasks = mock(TaskInternalApi.class);
        FocusSessionInternalApi sessions = mock(FocusSessionInternalApi.class);
        UserApplicationService service = new UserApplicationService(repository, new UserMapperImpl(), new UserService(repository), projects, tasks, sessions);
        when(repository.findById(1L)).thenReturn(Optional.of(new User("Student", "student@example.com", "test-hash")));
        when(projects.listarPorUsuario(1L)).thenReturn(List.of(new ProjectDTO(2L, 1L, "Project", null)));

        service.delete(1L);

        var order = inOrder(sessions, tasks, projects, repository);
        order.verify(sessions).excluirPorUsuario(1L);
        order.verify(tasks).excluirPorProjetos(1L, List.of(2L));
        order.verify(projects).excluirPorUsuario(1L);
        order.verify(repository).deleteById(1L);
    }

    @Test
    void checksCandidateBeforeMutatingManagedUserOnPutAndPatch() {
        UserRepository repository = mock(UserRepository.class);
        User current = new User("Original", "original@example.com", "test-hash");
        when(repository.findById(1L)).thenReturn(Optional.of(current));
        when(repository.existsByEmailAndIdNot("taken@example.com", 1L)).thenAnswer(call -> {
            assertEquals("Original", current.getName());
            assertEquals("original@example.com", current.getEmail());
            return true;
        });
        UserApplicationService service = new UserApplicationService(repository, new UserMapperImpl(),
                new UserService(repository), mock(ProjectInternalApi.class), mock(TaskInternalApi.class),
                mock(FocusSessionInternalApi.class));
        assertThrows(EmailAlreadyExistsException.class,
                () -> service.update(1L, new UserUpdateDTO("Changed", "TAKEN@example.com")));
        assertThrows(EmailAlreadyExistsException.class,
                () -> service.patch(1L, new UserPatchDTO("Changed", "TAKEN@example.com")));
        assertEquals("Original", current.getName());
        assertEquals("original@example.com", current.getEmail());
        verify(repository, never()).save(any());
    }

    @Test
    void readsAndUpdatesThroughMapperWithoutChangingIdentity() {
        UserRepository repository = mock(UserRepository.class);
        UserApplicationService service = new UserApplicationService(repository, new UserMapperImpl(),
                new UserService(repository), mock(ProjectInternalApi.class), mock(TaskInternalApi.class),
                mock(FocusSessionInternalApi.class));
        var created = new UserResponseDTO(1L, "Student", "student@example.com");
        User stored = new User(created.name(), created.email(), "test-hash");
        org.springframework.test.util.ReflectionTestUtils.setField(stored, "id", 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(stored));
        when(repository.findAll()).thenReturn(List.of(stored));
        assertEquals(created, service.getById(1L));
        assertEquals(List.of(created), service.getAll());
        var updated = service.update(1L, new UserUpdateDTO("Updated", "STUDENT@example.com"));
        assertEquals(1L, updated.id());
        assertEquals("student@example.com", updated.email());
        assertEquals(updated, service.patch(1L, new UserPatchDTO(null, null)));
        assertEquals("test-hash", stored.getPasswordHash());
        assertEquals(UserRole.USER, stored.getRole());
        verify(repository).existsByEmailAndIdNot("student@example.com", 1L);
        assertEquals("User not found",
                assertThrows(EntidadeNaoEncontradaException.class, () -> service.getById(99L)).getMessage());
        assertEquals("User not found",
                assertThrows(EntidadeNaoEncontradaException.class, () -> service.delete(99L)).getMessage());
    }
}
