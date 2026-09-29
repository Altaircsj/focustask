package br.edu.ufersa.pw.focustask.features.user;

import br.edu.ufersa.pw.focustask.shared.exception.EntidadeNaoEncontradaException;
import br.edu.ufersa.pw.focustask.features.user.dto.*;
import br.edu.ufersa.pw.focustask.features.project.ProjectDTO;
import br.edu.ufersa.pw.focustask.features.project.ProjectInternalApi;
import br.edu.ufersa.pw.focustask.features.task.TaskInternalApi;
import br.edu.ufersa.pw.focustask.features.focusSession.FocusSessionInternalApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
class UserApplicationService {
    private final UserRepository repository;
    private final UserMapper mapper;
    private final UserService domain;
    private final ProjectInternalApi projects;
    private final TaskInternalApi tasks;
    private final FocusSessionInternalApi sessions;

    UserApplicationService(UserRepository repository, UserMapper mapper, UserService domain,
                           ProjectInternalApi projects, TaskInternalApi tasks, FocusSessionInternalApi sessions) {
        this.repository = repository;
        this.mapper = mapper;
        this.domain = domain;
        this.projects = projects;
        this.tasks = tasks;
        this.sessions = sessions;
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAll() { return mapper.toResponseList(repository.findAll()); }

    @Transactional(readOnly = true)
    public UserResponseDTO getById(Long userId) { return mapper.toResponse(requireUser(userId)); }

    @Transactional
    public UserResponseDTO update(Long userId, UserUpdateDTO dto) {
        User current = requireUser(userId);
        // Validate a detached candidate before a uniqueness query can auto-flush managed changes.
        User candidate = current.copy();
        mapper.updateEntityFromDto(dto, candidate);
        domain.validarAtualizacao(candidate, userId);
        mapper.updateEntityFromDto(dto, current);
        return mapper.toResponse(current);
    }

    @Transactional
    public UserResponseDTO patch(Long userId, UserPatchDTO dto) {
        User current = requireUser(userId);
        User candidate = current.copy();
        mapper.patchEntityFromDto(dto, candidate);
        if (dto.email() != null) domain.validarAtualizacao(candidate, userId);
        mapper.patchEntityFromDto(dto, current);
        return mapper.toResponse(current);
    }

    @Transactional
    public void delete(Long userId) {
        requireUser(userId);
        List<Long> projectIds = projects.listarPorUsuario(userId).stream().map(ProjectDTO::id).toList();
        sessions.excluirPorUsuario(userId);
        tasks.excluirPorProjetos(userId, projectIds);
        projects.excluirPorUsuario(userId);
        // Bulk APIs clear the persistence context; continue using IDs.
        repository.deleteById(userId);
        repository.flush();
    }

    private User requireUser(Long userId) {
        return repository.findById(userId).orElseThrow(() -> new EntidadeNaoEncontradaException("User not found"));
    }
}
