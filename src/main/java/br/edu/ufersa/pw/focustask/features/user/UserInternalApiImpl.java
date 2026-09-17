package br.edu.ufersa.pw.focustask.features.user;

import org.springframework.stereotype.Service;

@Service
class UserInternalApiImpl implements UserInternalApi {

    private final UserRepository repository;

    UserInternalApiImpl(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDTO obterUsuarioPorId(Long id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        return new UserDTO(user.getId(), user.getName(), user.getEmail());
    }

    @Override
    public boolean existePorId(Long id) {
        return repository.existsById(id);
    }
}