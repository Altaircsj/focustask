package br.edu.ufersa.pw.focustask.features.task;

import java.util.List;

public interface TaskInternalApi {
    boolean pertenceAoUsuario(Long userId, Long taskId);
    List<Long> listarIdsPorProjetos(Long userId, List<Long> projectIds);

    /** Requires an existing transaction and previously detached/deleted focus sessions. */
    void excluirPorProjetos(Long userId, List<Long> projectIds);
}
