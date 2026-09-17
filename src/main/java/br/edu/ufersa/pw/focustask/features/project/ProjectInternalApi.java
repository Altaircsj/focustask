package br.edu.ufersa.pw.focustask.features.project;

import java.util.List;

public interface ProjectInternalApi {
    Project criarProjeto(Long userId, Project project);
    List<Project> listarPorUsuario(Long userId);
}