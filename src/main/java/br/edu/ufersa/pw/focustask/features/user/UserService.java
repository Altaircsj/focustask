package br.edu.ufersa.pw.focustask.features.user;

import br.edu.ufersa.pw.focustask.features.project.ProjectDTO;
import br.edu.ufersa.pw.focustask.features.project.ProjectInternalApi;
import br.edu.ufersa.pw.focustask.features.task.TaskInternalApi;
import br.edu.ufersa.pw.focustask.features.focusSession.FocusSessionInternalApi;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@Transactional(readOnly = true)
class UserService {
    private final UserRepository repository;
    private final ProjectInternalApi projectApi;
    private final TaskInternalApi taskApi;
    private final FocusSessionInternalApi sessionApi;

    UserService(UserRepository repository, ProjectInternalApi projectApi,
                TaskInternalApi taskApi, FocusSessionInternalApi sessionApi) {
        this.repository = repository;
        this.projectApi = projectApi;
        this.taskApi = taskApi;
        this.sessionApi = sessionApi;
    }

    public List<UserDTO> getAll() {
        return repository.findAll().stream().map(User::toDTO).toList();
    }

    public UserDTO getById(Long userId) { return requireUser(userId).toDTO(); }

    @Transactional
    public UserDTO create(String name, String email) {
        User user = new User(name, email);
        if (repository.existsByEmail(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        return repository.save(user).toDTO();
    }

    @Transactional
    public UserDTO update(Long userId, String name, String email) {
        User current = requireUser(userId);
        User values = new User(name, email);
        if (repository.existsByEmailAndIdNot(values.getEmail(), userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        current.setName(values.getName());
        current.setEmail(values.getEmail());
        return current.toDTO();
    }

    @Transactional
    public void delete(Long userId) {
        requireUser(userId);
        List<Long> projectIds = projectApi.listarPorUsuario(userId).stream().map(ProjectDTO::id).toList();
        sessionApi.excluirPorUsuario(userId);
        taskApi.excluirPorProjetos(userId, projectIds);
        projectApi.excluirPorUsuario(userId);
        // Bulk APIs clear the persistence context: use IDs, not previously loaded entities.
        repository.deleteById(userId);
        repository.flush();
    }

    private User requireUser(Long id) {
        return repository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
