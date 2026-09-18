package br.edu.ufersa.pw.focustask.features.user;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")).toDTO();
    }

    @Override
    public boolean existePorId(Long id) {
        return id != null && repository.existsById(id);
    }
}
