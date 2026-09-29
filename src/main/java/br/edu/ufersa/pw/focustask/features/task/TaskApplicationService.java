package br.edu.ufersa.pw.focustask.features.task;

import br.edu.ufersa.pw.focustask.shared.exception.EntidadeNaoEncontradaException;
import br.edu.ufersa.pw.focustask.features.task.dto.*;
import br.edu.ufersa.pw.focustask.features.project.ProjectDTO;
import br.edu.ufersa.pw.focustask.features.project.ProjectInternalApi;
import br.edu.ufersa.pw.focustask.features.focusSession.FocusSessionInternalApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
class TaskApplicationService {
    private final TaskRepository repository;
    private final TaskMapper mapper;
    private final TaskService domain;
    private final ProjectInternalApi projects;
    private final FocusSessionInternalApi sessions;

    TaskApplicationService(TaskRepository repository, TaskMapper mapper, TaskService domain,
                           ProjectInternalApi projects, FocusSessionInternalApi sessions) {
        this.repository = repository;
        this.mapper = mapper;
        this.domain = domain;
        this.projects = projects;
        this.sessions = sessions;
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDTO> getAll(Long userId) {
        List<Long> ids = projects.listarPorUsuario(userId).stream().map(ProjectDTO::id).toList();
        return ids.isEmpty() ? List.of() : mapper.toResponseList(repository.findAllByProjectIdIn(ids));
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDTO> getByProject(Long userId, Long projectId) {
        if (!projects.pertenceAoUsuario(userId, projectId)) throw new EntidadeNaoEncontradaException("Project not found");
        return mapper.toResponseList(repository.findAllByProjectId(projectId));
    }

    @Transactional(readOnly = true)
    public TaskResponseDTO getById(Long userId, Long taskId) {
        return mapper.toResponse(requireTask(userId, taskId));
    }

    @Transactional
    public TaskCreationResult create(Long projectId, TaskCreateDTO dto) {
        Long userId = projects.obterUsuarioIdPorProjeto(projectId);
        Task saved = repository.save(mapper.toEntity(dto, projectId));
        return new TaskCreationResult(userId, mapper.toResponse(saved));
    }

    @Transactional
    public TaskResponseDTO update(Long userId, Long taskId, TaskUpdateDTO dto) {
        Task task = requireTask(userId, taskId);
        domain.validarMovimentacao(task, userId, dto.projectId());
        mapper.updateEntityFromDto(dto, task);
        return mapper.toResponse(task);
    }

    @Transactional
    public TaskResponseDTO patch(Long userId, Long taskId, TaskPatchDTO dto) {
        Task task = requireTask(userId, taskId);
        if (dto.projectId() != null) domain.validarMovimentacao(task, userId, dto.projectId());
        mapper.patchEntityFromDto(dto, task);
        return mapper.toResponse(task);
    }

    @Transactional
    public void delete(Long userId, Long taskId) {
        requireTask(userId, taskId);
        sessions.desvincularDeTarefas(userId, List.of(taskId));
        repository.deleteById(taskId);
        repository.flush();
    }

    private Task requireTask(Long userId, Long taskId) {
        Task task = repository.findById(taskId).orElseThrow(() -> new EntidadeNaoEncontradaException("Task not found"));
        if (!projects.pertenceAoUsuario(userId, task.getProjectId())) throw new EntidadeNaoEncontradaException("Task not found");
        return task;
    }
}
