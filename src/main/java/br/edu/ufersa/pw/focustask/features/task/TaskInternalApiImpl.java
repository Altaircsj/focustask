package br.edu.ufersa.pw.focustask.features.task;

import br.edu.ufersa.pw.focustask.features.project.ProjectInternalApi;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@Transactional(readOnly = true)
class TaskInternalApiImpl implements TaskInternalApi {
    private final TaskRepository repository;
    private final ProjectInternalApi projectApi;

    TaskInternalApiImpl(TaskRepository repository, ProjectInternalApi projectApi) {
        this.repository = repository;
        this.projectApi = projectApi;
    }

    @Override
    public boolean pertenceAoUsuario(Long userId, Long taskId) {
        return userId != null && taskId != null && repository.findById(taskId)
                .map(task -> projectApi.pertenceAoUsuario(userId, task.getProjectId())).orElse(false);
    }

    @Override
    public List<Long> listarIdsPorProjetos(Long userId, List<Long> projectIds) {
        if (projectIds.isEmpty()) return List.of();
        requireOwnedProjects(userId, projectIds);
        return repository.findAllByProjectIdIn(projectIds).stream().map(Task::getId).toList();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void excluirPorProjetos(Long userId, List<Long> projectIds) {
        if (projectIds.isEmpty()) return;
        requireOwnedProjects(userId, projectIds);
        repository.deleteAllForProjects(projectIds);
    }

    private void requireOwnedProjects(Long userId, List<Long> projectIds) {
        for (Long projectId : projectIds) {
            if (!projectApi.pertenceAoUsuario(userId, projectId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
            }
        }
    }
}
