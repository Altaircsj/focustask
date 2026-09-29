package br.edu.ufersa.pw.focustask.features.project;

import br.edu.ufersa.pw.focustask.shared.exception.EntidadeNaoEncontradaException;
import br.edu.ufersa.pw.focustask.features.project.dto.*;
import br.edu.ufersa.pw.focustask.features.user.UserInternalApi;
import br.edu.ufersa.pw.focustask.features.task.TaskInternalApi;
import br.edu.ufersa.pw.focustask.features.focusSession.FocusSessionInternalApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
class ProjectApplicationService {
    private final ProjectRepository repository;
    private final ProjectMapper mapper;
    private final UserInternalApi users;
    private final TaskInternalApi tasks;
    private final FocusSessionInternalApi sessions;

    ProjectApplicationService(ProjectRepository repository, ProjectMapper mapper, UserInternalApi users,
                              TaskInternalApi tasks, FocusSessionInternalApi sessions) {
        this.repository = repository;
        this.mapper = mapper;
        this.users = users;
        this.tasks = tasks;
        this.sessions = sessions;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponseDTO> getAll(Long userId) {
        requireUser(userId);
        return mapper.toResponseList(repository.findAllByUserId(userId));
    }

    @Transactional(readOnly = true)
    public ProjectResponseDTO getById(Long userId, Long projectId) {
        return mapper.toResponse(requireProject(userId, projectId));
    }

    @Transactional
    public ProjectResponseDTO create(Long userId, ProjectCreateDTO dto) {
        requireUser(userId);
        return mapper.toResponse(repository.save(mapper.toEntity(dto, userId)));
    }

    @Transactional
    public ProjectResponseDTO update(Long userId, Long projectId, ProjectUpdateDTO dto) {
        Project project = requireProject(userId, projectId);
        mapper.updateEntityFromDto(dto, project);
        return mapper.toResponse(project);
    }

    @Transactional
    public ProjectResponseDTO patch(Long userId, Long projectId, ProjectPatchDTO dto) {
        Project project = requireProject(userId, projectId);
        mapper.patchEntityFromDto(dto, project);
        return mapper.toResponse(project);
    }

    @Transactional
    public void delete(Long userId, Long projectId) {
        requireProject(userId, projectId);
        List<Long> projectIds = List.of(projectId);
        List<Long> taskIds = tasks.listarIdsPorProjetos(userId, projectIds);
        sessions.desvincularDeTarefas(userId, taskIds);
        tasks.excluirPorProjetos(userId, projectIds);
        repository.deleteById(projectId);
        repository.flush();
    }

    private Project requireProject(Long userId, Long projectId) {
        return repository.findByIdAndUserId(projectId, userId).orElseThrow(() -> new EntidadeNaoEncontradaException("Project not found"));
    }

    private void requireUser(Long userId) {
        if (!users.existePorId(userId)) throw new EntidadeNaoEncontradaException("User not found");
    }
}
