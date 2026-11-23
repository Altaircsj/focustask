package br.edu.ufersa.pw.focustask.features.user;

public interface UserInternalApi {
    UserDTO obterUsuarioPorId(Long id);
    boolean existePorId(Long id);

    /**
     * Serializa, por usuário, as operações que dependem de um invariante entre várias linhas
     * (SELECT ... FOR UPDATE na linha do usuário). Exige transação ativa e deve ser a primeira leitura dela,
     * para que as leituras seguintes enxerguem o que a transação anterior confirmou (MySQL REPEATABLE READ).
     */
    void bloquearUsuario(Long id);
}