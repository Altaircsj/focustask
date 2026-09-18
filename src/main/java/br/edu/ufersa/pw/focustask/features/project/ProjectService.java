package br.edu.ufersa.pw.focustask.features.project;

import br.edu.ufersa.pw.focustask.features.task.TaskInternalApi;
import br.edu.ufersa.pw.focustask.features.focusSession.FocusSessionInternalApi;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@Transactional(readOnly = true)
class ProjectService {
    private final ProjectRepository repository;
    private final ProjectInternalApi projectApi;
    private final TaskInternalApi taskApi;
    private final FocusSessionInternalApi sessionApi;

    ProjectService(ProjectRepository repository, ProjectInternalApi projectApi,
                   TaskInternalApi taskApi, FocusSessionInternalApi sessionApi) {
        this.repository = repository;
        this.projectApi = projectApi;
        this.taskApi = taskApi;
        this.sessionApi = sessionApi;
    }

    public List<ProjectDTO> getAll(Long userId) { return projectApi.listarPorUsuario(userId); }
    public ProjectDTO getById(Long userId, Long projectId) { return requireProject(userId, projectId).toDTO(); }

    @Transactional
    public ProjectDTO create(Long userId, ProjectDTO dto) { return projectApi.criarProjeto(userId, dto); }

    @Transactional
    public ProjectDTO update(Long userId, Long projectId, String name, String description) {
        Project project = requireProject(userId, projectId);
        project.setName(name);
        project.setDescription(description);
        return project.toDTO();
    }

    @Transactional
    public void delete(Long userId, Long projectId) {
        requireProject(userId, projectId);
        List<Long> projectIds = List.of(projectId);
        List<Long> taskIds = taskApi.listarIdsPorProjetos(userId, projectIds);
        sessionApi.desvincularDeTarefas(userId, taskIds);
        taskApi.excluirPorProjetos(userId, projectIds);
        repository.deleteById(projectId);
        repository.flush();
    }

    private Project requireProject(Long userId, Long projectId) {
        return repository.findByIdAndUserId(projectId, userId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }
}
