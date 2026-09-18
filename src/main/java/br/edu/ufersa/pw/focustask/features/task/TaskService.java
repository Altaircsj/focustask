package br.edu.ufersa.pw.focustask.features.task;

import br.edu.ufersa.pw.focustask.features.project.ProjectDTO;
import br.edu.ufersa.pw.focustask.features.project.ProjectInternalApi;
import br.edu.ufersa.pw.focustask.features.focusSession.FocusSessionInternalApi;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
class TaskService {
    private final TaskRepository repository;
    private final ProjectInternalApi projectApi;
    private final FocusSessionInternalApi sessionApi;

    TaskService(TaskRepository repository, ProjectInternalApi projectApi,
                FocusSessionInternalApi sessionApi) {
        this.repository = repository;
        this.projectApi = projectApi;
        this.sessionApi = sessionApi;
    }

    public List<Task> getAll(Long userId) {
        List<Long> ids = projectApi.listarPorUsuario(userId).stream().map(ProjectDTO::id).toList();
        return ids.isEmpty() ? List.of() : repository.findAllByProjectIdIn(ids);
    }

    public List<Task> getByProject(Long userId, Long projectId) {
        requireProject(userId, projectId);
        return repository.findAllByProjectId(projectId);
    }

    public Task getById(Long userId, Long taskId) {
        Task task = repository.findById(taskId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        requireProject(userId, task.getProjectId());
        return task;
    }

    @Transactional
    public Task create(Long userId, Long projectId, String title, String description, LocalDate dueDate) {
        requireProject(userId, projectId);
        Task task = new Task(projectId, title);
        task.setDescription(description);
        task.setDueDate(dueDate);
        return repository.save(task);
    }

    @Transactional
    public Task update(Long userId, Long taskId, Long projectId, String title,
                       String description, TaskStatus status, TaskPriority priority, LocalDate dueDate) {
        Task task = getById(userId, taskId);
        requireProject(userId, projectId);
        task.setProjectId(projectId);
        task.setTitle(title);
        task.setDescription(description);
        task.setStatus(status);
        task.setPriority(priority);
        task.setDueDate(dueDate);
        return task;
    }

    @Transactional
    public void delete(Long userId, Long taskId) {
        getById(userId, taskId);
        sessionApi.desvincularDeTarefas(userId, List.of(taskId));
        repository.deleteById(taskId);
        repository.flush();
    }

    private void requireProject(Long userId, Long projectId) {
        if (!projectApi.pertenceAoUsuario(userId, projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
    }
}
