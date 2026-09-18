package br.edu.ufersa.pw.focustask.features.focusSession;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(propagation = Propagation.MANDATORY)
class FocusSessionInternalApiImpl implements FocusSessionInternalApi {
    private final FocusSessionRepository repository;

    FocusSessionInternalApiImpl(FocusSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    public void desvincularDeTarefas(Long userId, List<Long> taskIds) {
        if (!taskIds.isEmpty()) repository.detachFromTasks(userId, taskIds);
    }

    @Override
    public void excluirPorUsuario(Long userId) {
        repository.deleteAllForUser(userId);
    }
}
