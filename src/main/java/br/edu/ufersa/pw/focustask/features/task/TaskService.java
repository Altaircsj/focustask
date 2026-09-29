package br.edu.ufersa.pw.focustask.features.task;

import br.edu.ufersa.pw.focustask.shared.exception.EntidadeNaoEncontradaException;
import br.edu.ufersa.pw.focustask.features.project.ProjectInternalApi;
import org.springframework.stereotype.Service;

@Service
class TaskService {
    private final ProjectInternalApi projects;

    TaskService(ProjectInternalApi projects) { this.projects = projects; }

    /** The caller has already resolved the task within this user's context. */
    public void validarMovimentacao(Task task, Long userId, Long destinationProjectId) {
        if (!task.getProjectId().equals(destinationProjectId)
                && !projects.pertenceAoUsuario(userId, destinationProjectId)) {
            throw new EntidadeNaoEncontradaException("Project not found");
        }
    }
}
