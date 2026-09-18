package br.edu.ufersa.pw.focustask.features.focusSession;

import br.edu.ufersa.pw.focustask.features.task.TaskInternalApi;
import br.edu.ufersa.pw.focustask.features.user.UserInternalApi;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.util.List;

@Service
@Transactional(readOnly = true)
class FocusSessionService {
    private final FocusSessionRepository repository;
    private final UserInternalApi userApi;
    private final TaskInternalApi taskApi;
    private final Clock clock;

    FocusSessionService(FocusSessionRepository repository, UserInternalApi userApi,
                        TaskInternalApi taskApi, Clock clock) {
        this.repository = repository;
        this.userApi = userApi;
        this.taskApi = taskApi;
        this.clock = clock;
    }

    public List<FocusSession> getAll(Long userId) {
        requireUser(userId);
        return repository.findAllByUserId(userId);
    }

    public List<FocusSession> getByTask(Long userId, Long taskId) {
        requireTask(userId, taskId);
        return repository.findAllByTaskIdAndUserId(taskId, userId);
    }

    public FocusSession getById(Long userId, Long sessionId) {
        return repository.findByIdAndUserId(sessionId, userId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Focus session not found"));
    }

    @Transactional
    public FocusSession create(Long userId, Long taskId) {
        requireUser(userId);
        if (taskId != null) requireTask(userId, taskId);
        FocusSession session = new FocusSession(userId, clock);
        session.setTaskId(taskId);
        return repository.save(session);
    }

    @Transactional
    public FocusSession linkTask(Long userId, Long sessionId, Long taskId) {
        FocusSession session = forUpdate(userId, sessionId);
        if (taskId != null) requireTask(userId, taskId);
        session.setTaskId(taskId);
        return session;
    }

    @Transactional
    public FocusSession pause(Long userId, Long sessionId) {
        FocusSession session = forUpdate(userId, sessionId);
        session.pause(clock);
        return session;
    }

    @Transactional
    public FocusSession resume(Long userId, Long sessionId) {
        FocusSession session = forUpdate(userId, sessionId);
        session.resume(clock);
        return session;
    }

    @Transactional
    public FocusSession complete(Long userId, Long sessionId) {
        FocusSession session = forUpdate(userId, sessionId);
        session.complete(clock);
        return session;
    }

    @Transactional
    public void delete(Long userId, Long sessionId) {
        repository.delete(forUpdate(userId, sessionId));
    }

    private FocusSession forUpdate(Long userId, Long sessionId) {
        return repository.findForUpdate(sessionId, userId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Focus session not found"));
    }

    private void requireUser(Long userId) {
        if (!userApi.existePorId(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
    }

    private void requireTask(Long userId, Long taskId) {
        if (!taskApi.pertenceAoUsuario(userId, taskId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }
    }
}
