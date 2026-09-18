package br.edu.ufersa.pw.focustask.features.project;

import br.edu.ufersa.pw.focustask.features.user.UserInternalApi;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@Transactional(readOnly = true)
class ProjectInternalApiImpl implements ProjectInternalApi {
    private final ProjectRepository repository;
    private final UserInternalApi userApi;

    ProjectInternalApiImpl(ProjectRepository repository, UserInternalApi userApi) {
        this.repository = repository;
        this.userApi = userApi;
    }

    @Override
    @Transactional
    public ProjectDTO criarProjeto(Long userId, ProjectDTO dto) {
        requireUser(userId);
        // Ownership and generated ID come from the server, not from DTO fields.
        return repository.save(new Project(userId, dto.name(), dto.description())).toDTO();
    }

    @Override
    public List<ProjectDTO> listarPorUsuario(Long userId) {
        requireUser(userId);
        return repository.findAllByUserId(userId).stream().map(Project::toDTO).toList();
    }

    @Override
    public boolean pertenceAoUsuario(Long userId, Long projectId) {
        return userId != null && projectId != null
                && repository.findByIdAndUserId(projectId, userId).isPresent();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void excluirPorUsuario(Long userId) {
        requireUser(userId);
        repository.deleteAllForUser(userId);
    }

    private void requireUser(Long userId) {
        if (!userApi.existePorId(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
    }
}
