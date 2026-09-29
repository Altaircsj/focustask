package br.edu.ufersa.pw.focustask.features.user;

import br.edu.ufersa.pw.focustask.shared.exception.EntidadeNaoEncontradaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class UserInternalApiImpl implements UserInternalApi {
    private final UserRepository repository;

    UserInternalApiImpl(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDTO obterUsuarioPorId(Long id) {
        return repository.findById(id).orElseThrow(
                () -> new EntidadeNaoEncontradaException("User not found")).toDTO();
    }

    @Override
    public boolean existePorId(Long id) {
        return id != null && repository.existsById(id);
    }
}
