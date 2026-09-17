package br.edu.ufersa.pw.focustask.features.focusSession;

import br.edu.ufersa.pw.focustask.features.task.Task;
import br.edu.ufersa.pw.focustask.features.user.User;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class FocusSessionTests {

    private static final Instant START = Instant.parse("2026-09-17T12:00:00Z");

    @Test
    void multiplePausesAccumulateWithoutCountingRepeatedTransitions() {
        FocusSession session = new FocusSession(mock(User.class), at(0));

        session.pause(at(60));
        session.pause(at(90));
        session.resume(at(120));
        session.resume(at(150));
        session.pause(at(180));
        session.resume(at(210));
        session.complete(at(300));

        assertEquals(90, session.getTotalPausedSeconds());
        assertEquals(FocusSessionStatus.COMPLETED, session.getStatus());
        assertEquals(START, session.getStartedAt());
        assertEquals(START.plusSeconds(300), session.getEndedAt());
        assertNull(session.getPausedAt());
    }

    @Test
    void completingWhilePausedIncludesTheOpenPauseOnlyOnce() {
        FocusSession session = new FocusSession(mock(User.class), at(0));
        session.pause(at(60));

        session.complete(at(100));
        session.complete(at(150));

        assertEquals(40, session.getTotalPausedSeconds());
        assertEquals(START.plusSeconds(100), session.getEndedAt());
        assertNull(session.getPausedAt());
    }

    @Test
    void completedSessionCannotBePausedOrResumed() {
        FocusSession session = new FocusSession(mock(User.class), at(0));
        session.complete(at(60));

        assertThrows(IllegalStateException.class, () -> session.pause(at(90)));
        assertThrows(IllegalStateException.class, () -> session.resume(at(90)));
        assertEquals(FocusSessionStatus.COMPLETED, session.getStatus());
    }

    @Test
    void standaloneSessionCanBeLinkedAndDetachedAfterCompletionWithoutLosingHistory() {
        User user = mock(User.class);
        Task task = mock(Task.class);
        FocusSession session = new FocusSession(user, at(0));
        assertNull(session.getTask());
        session.pause(at(30));
        session.complete(at(60));

        session.setTask(task);
        assertSame(task, session.getTask());
        session.setTask(null);

        assertNull(session.getTask());
        assertSame(user, session.getUser());
        assertEquals(FocusSessionStatus.COMPLETED, session.getStatus());
        assertEquals(START, session.getStartedAt());
        assertEquals(START.plusSeconds(60), session.getEndedAt());
        assertEquals(30, session.getTotalPausedSeconds());
    }

    @Test
    void rejectsEventsBeforeSessionStartWithoutChangingState() {
        FocusSession session = new FocusSession(mock(User.class), at(0));

        assertThrows(IllegalArgumentException.class, () -> session.pause(at(-1)));
        assertThrows(IllegalArgumentException.class, () -> session.complete(at(-1)));
        assertEquals(FocusSessionStatus.RUNNING, session.getStatus());
        assertNull(session.getPausedAt());
        assertNull(session.getEndedAt());
    }

    @Test
    void rejectsFinishingAPauseBeforeItStartedWithoutChangingState() {
        FocusSession session = new FocusSession(mock(User.class), at(0));
        session.pause(at(60));

        assertThrows(IllegalArgumentException.class, () -> session.resume(at(59)));
        assertThrows(IllegalArgumentException.class, () -> session.complete(at(59)));
        assertEquals(FocusSessionStatus.PAUSED, session.getStatus());
        assertEquals(START.plusSeconds(60), session.getPausedAt());
        assertEquals(0, session.getTotalPausedSeconds());
        assertNull(session.getEndedAt());
    }

    @Test
    void rejectsNegativeActiveDurationAfterAnEarlierPause() {
        FocusSession session = new FocusSession(mock(User.class), at(0));
        session.pause(at(10));
        session.resume(at(100));

        assertThrows(IllegalArgumentException.class, () -> session.complete(at(80)));
        assertEquals(FocusSessionStatus.RUNNING, session.getStatus());
        assertEquals(90, session.getTotalPausedSeconds());
        assertNull(session.getEndedAt());
    }

    private static Clock at(long seconds) {
        return Clock.fixed(START.plusSeconds(seconds), ZoneOffset.UTC);
    }
}
