package br.edu.ufersa.pw.focustask.features.focusSession;

import br.edu.ufersa.pw.focustask.features.focusSession.dto.FocusSessionCreateDTO;
import br.edu.ufersa.pw.focustask.features.focusSession.dto.FocusSessionPatchDTO;
import br.edu.ufersa.pw.focustask.features.focusSession.dto.FocusSessionStatusDTO;
import br.edu.ufersa.pw.focustask.features.focusSession.dto.FocusSessionUpdateDTO;
import br.edu.ufersa.pw.focustask.features.project.ProjectInternalApi;
import br.edu.ufersa.pw.focustask.features.task.TaskInternalApi;
import br.edu.ufersa.pw.focustask.features.user.UserInternalApi;
import br.edu.ufersa.pw.focustask.shared.exception.EntidadeNaoEncontradaException;
import br.edu.ufersa.pw.focustask.shared.exception.OperacaoInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Regra do Plano A: no máximo uma sessão RUNNING por usuário, com troca atômica no Play. */
class FocusSessionSwitchTests {
    private static final Instant T0 = Instant.parse("2026-09-17T12:00:00Z");
    private static final long USER = 1L;

    private final FocusSessionRepository repository = mock(FocusSessionRepository.class);
    private final UserInternalApi users = mock(UserInternalApi.class);
    private final TaskInternalApi tasks = mock(TaskInternalApi.class);
    private final ProjectInternalApi projects = mock(ProjectInternalApi.class);
    private final List<FocusSession> store = new ArrayList<>();
    private Instant now = T0;
    private long nextId = 1;
    private final Clock clock = new Clock() {
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    };
    private FocusSessionApplicationService service;

    @BeforeEach
    void setUp() {
        when(repository.save(any())).thenAnswer(call -> {
            FocusSession session = call.getArgument(0);
            if (session.getId() == null) {
                ReflectionTestUtils.setField(session, "id", nextId++);
                store.add(session);
            }
            return session;
        });
        when(repository.findForUpdate(anyLong(), anyLong())).thenAnswer(call -> store.stream()
                .filter(s -> Objects.equals(s.getId(), call.getArgument(0)) && Objects.equals(s.getUserId(), call.getArgument(1)))
                .findFirst());
        when(repository.findAllForUpdateByUserIdAndStatus(anyLong(), any())).thenAnswer(call -> store.stream()
                .filter(s -> Objects.equals(s.getUserId(), call.getArgument(0)) && Objects.equals(s.getStatus(), call.getArgument(1)))
                .toList());
        service = new FocusSessionApplicationService(repository, new FocusSessionMapperImpl(),
                new FocusSessionService(tasks, projects), users, tasks, clock);
    }

    @Test
    void playOnAnotherSessionPausesTheRunningOneAndResumesItWhereItStopped() {
        long a = create(USER);
        advanceMinutes(10);
        long b = create(USER);
        assertEquals(byId(b).getStartedAt(), byId(a).getPausedAt(), "A é pausada no mesmo instante em que B começa");
        advanceMinutes(5);

        play(USER, a);

        FocusSession sa = byId(a);
        FocusSession sb = byId(b);
        assertEquals(List.of(sa), running(USER));
        assertEquals(FocusSessionStatus.PAUSED, sb.getStatus());
        assertEquals(600, activeSeconds(sa), "A tem 10 minutos ativos");
        assertEquals(300, activeSeconds(sb), "B tem 5 minutos ativos");
        assertEquals(300, sa.getTotalPausedSeconds());
        assertEquals(now, sb.getPausedAt(), "B é pausada no mesmo instante em que A retoma");
        assertEquals(now, sa.getLastActivatedAt());
    }

    @Test
    void creatingFromATaskAlsoPausesTheRunningSession() {
        when(tasks.pertenceAoUsuario(USER, 5L)).thenReturn(true);
        long a = create(USER);
        advanceMinutes(3);

        long b = service.createForTask(USER, 5L).id();

        assertEquals(FocusSessionStatus.PAUSED, byId(a).getStatus());
        assertEquals(List.of(byId(b)), running(USER));
        assertEquals(5L, byId(b).getTaskId());
    }

    @Test
    void putAndPatchThatRequestRunningSwitchTheSession() {
        long a = create(USER);
        advanceMinutes(1);
        long b = create(USER);
        advanceMinutes(1);
        service.update(USER, a, new FocusSessionUpdateDTO(null, FocusSessionStatusDTO.RUNNING));
        assertEquals(List.of(byId(a)), running(USER));
        advanceMinutes(1);
        service.patch(USER, b, new FocusSessionPatchDTO(null, FocusSessionStatusDTO.RUNNING));
        assertEquals(List.of(byId(b)), running(USER));
        assertEquals(FocusSessionStatus.PAUSED, byId(a).getStatus());
    }

    @Test
    void repeatingPlayOnTheRunningSessionKeepsItsTimes() {
        long a = create(USER);
        advanceMinutes(3);
        pause(USER, a);
        advanceMinutes(2);
        play(USER, a);
        FocusSession session = byId(a);
        Instant activated = session.getLastActivatedAt();
        long paused = session.getTotalPausedSeconds();
        advanceMinutes(4);

        play(USER, a);

        assertEquals(FocusSessionStatus.RUNNING, session.getStatus());
        assertEquals(activated, session.getLastActivatedAt());
        assertEquals(paused, session.getTotalPausedSeconds());
        assertNull(session.getPausedAt());
    }

    @Test
    void invalidTargetLeavesTheRunningSessionUntouched() {
        long done = create(USER);
        service.patch(USER, done, new FocusSessionPatchDTO(null, FocusSessionStatusDTO.COMPLETED));
        long a = create(USER);
        advanceMinutes(2);

        assertEquals("A completed session cannot be resumed",
                assertThrows(OperacaoInvalidaException.class, () -> play(USER, done)).getMessage());

        assertEquals(List.of(byId(a)), running(USER));
        assertNull(byId(a).getPausedAt());
        assertEquals(0, byId(a).getTotalPausedSeconds());
    }

    @Test
    void anotherAccountsSessionCannotBeActivatedAndDoesNotInterfere() {
        long mine = create(USER);
        long theirs = create(2L);
        advanceMinutes(2);

        assertEquals("Focus session not found",
                assertThrows(EntidadeNaoEncontradaException.class, () -> play(USER, theirs)).getMessage());

        assertEquals(List.of(byId(mine)), running(USER));
        assertEquals(List.of(byId(theirs)), running(2L));
        long second = create(2L);
        assertEquals(List.of(byId(second)), running(2L));
        assertEquals(List.of(byId(mine)), running(USER), "criar na outra conta não pausa a minha");
    }

    @Test
    void pausingOrCompletingNeverStartsAnotherSession() {
        long a = create(USER);
        advanceMinutes(1);
        long b = create(USER);
        advanceMinutes(1);
        play(USER, a);
        advanceMinutes(1);

        service.patch(USER, b, new FocusSessionPatchDTO(null, FocusSessionStatusDTO.COMPLETED));
        assertEquals(List.of(byId(a)), running(USER), "concluir B pausada não mexe em A");
        assertEquals(FocusSessionStatus.COMPLETED, byId(b).getStatus());

        pause(USER, a);
        assertEquals(List.of(), running(USER));
        service.patch(USER, a, new FocusSessionPatchDTO(null, FocusSessionStatusDTO.COMPLETED));
        assertEquals(List.of(), running(USER));
    }

    @Test
    void lastPlayIsTrackedPerSessionEvenWhenAnOlderOneIsResumed() {
        long a = create(USER);
        advanceMinutes(1);
        long b = create(USER);
        advanceMinutes(1);
        play(USER, a);
        pause(USER, a);

        assertTrue(byId(a).getLastActivatedAt().isAfter(byId(b).getLastActivatedAt()));
        assertTrue(byId(b).getStartedAt().isAfter(byId(a).getStartedAt()));
        assertEquals(List.of(), running(USER));
        Instant lastPlay = byId(a).getLastActivatedAt();
        service.patch(USER, a, new FocusSessionPatchDTO(null, FocusSessionStatusDTO.COMPLETED));
        assertEquals(lastPlay, byId(a).getLastActivatedAt(), "concluir não altera o último Play");
        assertEquals(List.of(), running(USER), "concluir não inicia outra sessão");
    }

    @Test
    void activationAlsoPausesEverySessionLeftRunningByLegacyAccounts() {
        long legacy1 = create(USER);
        long legacy2 = rawRunning(USER);
        long target = rawRunning(USER);
        pauseDirectly(target);
        advanceMinutes(4);

        play(USER, target);

        assertEquals(List.of(byId(target)), running(USER));
        assertEquals(FocusSessionStatus.PAUSED, byId(legacy1).getStatus());
        assertEquals(FocusSessionStatus.PAUSED, byId(legacy2).getStatus());
    }

    @Test
    void takesTheUserLockBeforeAnyOtherAccess() {
        long a = create(USER);
        long b = create(USER);
        org.mockito.Mockito.clearInvocations(users, repository);

        play(USER, a);

        InOrder order = inOrder(users, repository);
        order.verify(users).bloquearUsuario(USER);
        order.verify(repository).findForUpdate(a, USER);
        order.verify(repository).findAllForUpdateByUserIdAndStatus(USER, FocusSessionStatus.RUNNING);
        assertEquals(FocusSessionStatus.PAUSED, byId(b).getStatus());

        org.mockito.Mockito.clearInvocations(users, repository);
        create(USER);
        InOrder creation = inOrder(users, repository);
        creation.verify(users).bloquearUsuario(USER);
        creation.verify(repository).findAllForUpdateByUserIdAndStatus(USER, FocusSessionStatus.RUNNING);
    }

    // --- helpers ---

    private long create(long userId) {
        return service.create(userId, new FocusSessionCreateDTO(null)).id();
    }

    /** Sessão RUNNING inserida sem passar pela regra, como existia antes do Plano A. */
    private long rawRunning(long userId) {
        FocusSession session = new FocusSession(userId, clock);
        ReflectionTestUtils.setField(session, "id", nextId++);
        store.add(session);
        return session.getId();
    }

    private void pauseDirectly(long id) {
        byId(id).pause(clock);
    }

    private void play(long userId, long id) {
        service.patch(userId, id, new FocusSessionPatchDTO(null, FocusSessionStatusDTO.RUNNING));
    }

    private void pause(long userId, long id) {
        service.patch(userId, id, new FocusSessionPatchDTO(null, FocusSessionStatusDTO.PAUSED));
    }

    private void advanceMinutes(long minutes) {
        now = now.plus(Duration.ofMinutes(minutes));
    }

    private FocusSession byId(long id) {
        return store.stream().filter(s -> s.getId() == id).findFirst().orElseThrow();
    }

    private List<FocusSession> running(long userId) {
        return store.stream()
                .filter(s -> s.getUserId() == userId && s.getStatus() == FocusSessionStatus.RUNNING)
                .toList();
    }

    private long activeSeconds(FocusSession session) {
        Instant end = session.getEndedAt() != null ? session.getEndedAt()
                : session.getPausedAt() != null ? session.getPausedAt() : now;
        return Duration.between(session.getStartedAt(), end).getSeconds() - session.getTotalPausedSeconds();
    }
}
