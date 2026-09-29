package br.edu.ufersa.pw.focustask.features.project;

import br.edu.ufersa.pw.focustask.shared.exception.EntidadeNaoEncontradaException;
import java.util.List;

public interface ProjectInternalApi {
    ProjectDTO criarProjeto(Long userId, ProjectDTO dto);
    List<ProjectDTO> listarPorUsuario(Long userId);
    boolean pertenceAoUsuario(Long userId, Long projectId);

    /**
     * Returns the stored owner ID.
     * @throws EntidadeNaoEncontradaException with "Project not found" if the project does not exist
     */
    Long obterUsuarioIdPorProjeto(Long projectId);

    /** Requires an existing transaction; delete the user's tasks before calling. */
    void excluirPorUsuario(Long userId);
}
