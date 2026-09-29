package br.edu.ufersa.pw.focustask.features.user;

import org.springframework.stereotype.Service;

/** Rules that require comparing a user with other persisted users. */
@Service
class UserService {
    private final UserRepository repository;

    UserService(UserRepository repository) { this.repository = repository; }

    public void validarCriacao(User user) {
        if (repository.existsByEmail(user.getEmail())) throw new EmailAlreadyExistsException();
    }

    public void validarAtualizacao(User candidate, Long userId) {
        if (repository.existsByEmailAndIdNot(candidate.getEmail(), userId)) {
            throw new EmailAlreadyExistsException();
        }
    }
}
