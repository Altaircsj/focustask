package br.edu.ufersa.pw.focustask.features.focusSession;

import java.util.List;

public interface FocusSessionInternalApi {
    /** Requires an existing transaction and task IDs validated by the calling feature. */
    void desvincularDeTarefas(Long userId, List<Long> taskIds);

    /** Requires an existing transaction coordinated by the user deletion service. */
    void excluirPorUsuario(Long userId);
}
