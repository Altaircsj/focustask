package br.edu.ufersa.pw.focustask.features.focusSession;

import br.edu.ufersa.pw.focustask.shared.exception.EntidadeNaoEncontradaException;
import br.edu.ufersa.pw.focustask.features.focusSession.dto.*;
import br.edu.ufersa.pw.focustask.features.user.UserInternalApi;
import br.edu.ufersa.pw.focustask.features.task.TaskInternalApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.List;

@Service
class FocusSessionApplicationService {
    private final FocusSessionRepository repository;
    private final FocusSessionMapper mapper;
    private final FocusSessionService domain;
    private final UserInternalApi users;
    private final TaskInternalApi tasks;
    private final Clock clock;

    FocusSessionApplicationService(FocusSessionRepository repository, FocusSessionMapper mapper,
                                   FocusSessionService domain, UserInternalApi users, TaskInternalApi tasks, Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.domain = domain;
        this.users = users;
        this.tasks = tasks;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<FocusSessionResponseDTO> getAll(Long userId) {
        requireUser(userId);
        return mapper.toResponseList(repository.findAllByUserId(userId));
    }

    @Transactional(readOnly = true)
    public List<FocusSessionResponseDTO> getByTask(Long userId, Long taskId) {
        if (!tasks.pertenceAoUsuario(userId, taskId)) throw new EntidadeNaoEncontradaException("Task not found");
        return mapper.toResponseList(repository.findAllByTaskIdAndUserId(taskId, userId));
    }

    @Transactional(readOnly = true)
    public FocusSessionResponseDTO getById(Long userId, Long sessionId) {
        return mapper.toResponse(repository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Focus session not found")));
    }

    @Transactional
    public FocusSessionResponseDTO create(Long userId, FocusSessionCreateDTO dto) {
        return createSession(userId, dto.taskId());
    }

    @Transactional
    public FocusSessionResponseDTO createForTask(Long userId, Long taskId) {
        return createSession(userId, taskId);
    }

    private FocusSessionResponseDTO createSession(Long userId, Long taskId) {
        requireUser(userId);
        domain.validarVinculo(userId, taskId);
        FocusSession session = new FocusSession(userId, clock);
        session.setTaskId(taskId);
        return mapper.toResponse(repository.save(session));
    }

    @Transactional
    public FocusSessionResponseDTO update(Long userId, Long sessionId, FocusSessionUpdateDTO dto) {
        FocusSession session = forUpdate(userId, sessionId);
        domain.validarVinculo(userId, dto.taskId());
        changeStatus(session, dto.status());
        session.setTaskId(dto.taskId());
        return mapper.toResponse(session);
    }

    @Transactional
    public FocusSessionResponseDTO patch(Long userId, Long sessionId, FocusSessionPatchDTO dto) {
        FocusSession session = forUpdate(userId, sessionId);
        if (dto.taskId() != null) domain.validarVinculo(userId, dto.taskId());
        if (dto.status() != null) changeStatus(session, dto.status());
        if (dto.taskId() != null) session.setTaskId(dto.taskId());
        return mapper.toResponse(session);
    }

    @Transactional
    public void delete(Long userId, Long sessionId) {
        repository.delete(forUpdate(userId, sessionId));
    }

    private void changeStatus(FocusSession session, FocusSessionStatusDTO status) {
        switch (status) {
            case RUNNING -> session.resume(clock);
            case PAUSED -> session.pause(clock);
            case COMPLETED -> session.complete(clock);
        }
    }

    private FocusSession forUpdate(Long userId, Long sessionId) {
        return repository.findForUpdate(sessionId, userId).orElseThrow(() -> new EntidadeNaoEncontradaException("Focus session not found"));
    }

    private void requireUser(Long userId) {
        if (!users.existePorId(userId)) throw new EntidadeNaoEncontradaException("User not found");
    }
}
