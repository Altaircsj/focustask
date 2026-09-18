package br.edu.ufersa.pw.focustask.features.project;

import java.util.List;

public interface ProjectInternalApi {
    ProjectDTO criarProjeto(Long userId, ProjectDTO dto);
    List<ProjectDTO> listarPorUsuario(Long userId);
    boolean pertenceAoUsuario(Long userId, Long projectId);

    /** Requires an existing transaction; delete the user's tasks before calling. */
    void excluirPorUsuario(Long userId);
}
