package br.edu.ufersa.pw.focustask.features.focusSession;

import br.edu.ufersa.pw.focustask.shared.exception.EntidadeNaoEncontradaException;
import br.edu.ufersa.pw.focustask.shared.exception.OperacaoInvalidaException;
import br.edu.ufersa.pw.focustask.features.focusSession.dto.*;
import br.edu.ufersa.pw.focustask.features.user.UserInternalApi;
import br.edu.ufersa.pw.focustask.features.task.TaskInternalApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.ZoneOffset;
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

    /** A nova sessão nasce RUNNING; a que estava rodando é pausada no mesmo instante e na mesma transação. */
    @Transactional
    public FocusSessionResponseDTO create(Long userId, FocusSessionCreateDTO dto) {
        lockUser(userId);
        domain.validarVinculo(userId, dto.taskId(), dto.projectId());
        Clock moment = moment();
        FocusSession session = new FocusSession(userId, moment);
        if (dto.projectId() != null) session.setDirectProject(dto.projectId());
        else session.setTaskId(dto.taskId());
        session.setTitle(dto.title());
        if (dto.plannedMinutes() != null) session.planDuration(dto.plannedMinutes(), moment);
        pauseOthers(userId, session, moment);
        return mapper.toResponse(repository.save(session));
    }

    @Transactional
    public FocusSessionResponseDTO createForTask(Long userId, Long taskId) {
        return create(userId, new FocusSessionCreateDTO(taskId));
    }

    @Transactional
    public FocusSessionResponseDTO update(Long userId, Long sessionId, FocusSessionUpdateDTO dto) {
        lockUser(userId);
        FocusSession session = forUpdate(userId, sessionId);
        domain.validarVinculo(userId, dto.taskId(), null);
        changeStatus(userId, session, dto.status(), moment());
        session.setTaskId(dto.taskId());
        return mapper.toResponse(session);
    }

    @Transactional
    public FocusSessionResponseDTO patch(Long userId, Long sessionId, FocusSessionPatchDTO dto) {
        lockUser(userId);
        FocusSession session = forUpdate(userId, sessionId);
        Clock moment = moment();
        FocusSessionAssociationDTO association = dto.association();
        if (dto.taskId() != null && association != null) {
            throw new OperacaoInvalidaException("Envie apenas uma associação.");
        }
        if (dto.taskId() != null) association = new FocusSessionAssociationDTO(dto.taskId(), null);
        if (association != null) domain.validarVinculo(userId, association.taskId(), association.projectId());
        if (dto.status() != null) changeStatus(userId, session, dto.status(), moment);
        if (dto.plannedMinutes() != null) session.planDuration(dto.plannedMinutes(), moment);
        if (dto.title() != null) session.setTitle(dto.title());
        if (association != null) {
            if (association.taskId() != null) session.setTaskId(association.taskId());
            else session.setDirectProject(association.projectId());
        }
        return mapper.toResponse(session);
    }

    @Transactional
    public void delete(Long userId, Long sessionId) {
        lockUser(userId);
        repository.delete(forUpdate(userId, sessionId));
    }

    private void changeStatus(Long userId, FocusSession session, FocusSessionStatusDTO status, Clock moment) {
        switch (status) {
            case RUNNING -> activate(userId, session, moment);
            case PAUSED -> session.pause(moment);
            case COMPLETED -> session.complete(moment);
        }
    }

    /**
     * Play: valida o destino antes de mexer em qualquer outra sessão, pausa as demais que estiverem
     * rodando e retoma a escolhida, tudo com o mesmo instante. Repetir Play numa sessão que já roda
     * não altera os tempos dela.
     */
    private void activate(Long userId, FocusSession session, Clock moment) {
        session.requireActivatable();
        pauseOthers(userId, session, moment);
        session.resume(moment);
    }

    private void pauseOthers(Long userId, FocusSession keep, Clock moment) {
        for (FocusSession other : repository.findAllForUpdateByUserIdAndStatus(userId, FocusSessionStatus.RUNNING)) {
            if (!isSame(other, keep)) other.pause(moment);
        }
    }

    private static boolean isSame(FocusSession a, FocusSession b) {
        return a == b || (a.getId() != null && a.getId().equals(b.getId()));
    }

    /** Um único instante do servidor para toda a operação, para que pausar A e ativar B coincidam. */
    private Clock moment() {
        return Clock.fixed(clock.instant(), ZoneOffset.UTC);
    }

    /**
     * Coordena as escritas por usuário: bloqueia a linha do usuário antes de qualquer outra leitura.
     * Bloquear apenas a linha da sessão destino não impediria duas abas de ativarem sessões diferentes.
     */
    private void lockUser(Long userId) {
        users.bloquearUsuario(userId);
    }

    private FocusSession forUpdate(Long userId, Long sessionId) {
        return repository.findForUpdate(sessionId, userId).orElseThrow(() -> new EntidadeNaoEncontradaException("Focus session not found"));
    }

    private void requireUser(Long userId) {
        if (!users.existePorId(userId)) throw new EntidadeNaoEncontradaException("User not found");
    }
}
