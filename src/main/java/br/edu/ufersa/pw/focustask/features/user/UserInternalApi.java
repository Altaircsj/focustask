package br.edu.ufersa.pw.focustask.features.user;

public interface UserInternalApi {
    UserDTO obterUsuarioPorId(Long id);
    boolean existePorId(Long id);
}