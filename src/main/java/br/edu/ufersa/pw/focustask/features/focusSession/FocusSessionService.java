package br.edu.ufersa.pw.focustask.features.focusSession;

import br.edu.ufersa.pw.focustask.shared.exception.EntidadeNaoEncontradaException;
import br.edu.ufersa.pw.focustask.features.task.TaskInternalApi;
import org.springframework.stereotype.Service;

@Service
class FocusSessionService {
    private final TaskInternalApi tasks;

    FocusSessionService(TaskInternalApi tasks) { this.tasks = tasks; }

    public void validarVinculo(Long userId, Long taskId) {
        if (taskId != null && !tasks.pertenceAoUsuario(userId, taskId)) {
            throw new EntidadeNaoEncontradaException("Task not found");
        }
    }
}
