package br.edu.ufersa.pw.focustask.features.focusSession;

import br.edu.ufersa.pw.focustask.shared.exception.EntidadeNaoEncontradaException;
import br.edu.ufersa.pw.focustask.features.task.TaskInternalApi;
import br.edu.ufersa.pw.focustask.features.project.ProjectInternalApi;
import br.edu.ufersa.pw.focustask.shared.exception.OperacaoInvalidaException;
import org.springframework.stereotype.Service;

@Service
class FocusSessionService {
    private final TaskInternalApi tasks;

    private final ProjectInternalApi projects;

    FocusSessionService(TaskInternalApi tasks, ProjectInternalApi projects) {
        this.tasks = tasks;
        this.projects = projects;
    }

    public void validarVinculo(Long userId, Long taskId, Long projectId) {
        if (taskId != null && projectId != null) {
            throw new OperacaoInvalidaException("Selecione uma tarefa ou um projeto.");
        }
        if (projectId != null && !projects.pertenceAoUsuario(userId, projectId)) {
            throw new EntidadeNaoEncontradaException("Project not found");
        }
        if (taskId != null && !tasks.pertenceAoUsuario(userId, taskId)) {
            throw new EntidadeNaoEncontradaException("Task not found");
        }
    }
}
