package br.edu.ufersa.pw.focustask.features.focusSession;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "focus_sessions", indexes = {
        @Index(name = "idx_focus_sessions_user", columnList = "user_id"),
        @Index(name = "idx_focus_sessions_task", columnList = "task_id")
})
class FocusSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "task_id")
    private Long taskId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private FocusSessionStatus status = FocusSessionStatus.RUNNING;

    @NotNull
    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "paused_at")
    private Instant pausedAt;

    @PositiveOrZero
    @Column(name = "total_paused_seconds", nullable = false)
    private long totalPausedSeconds = 0;

    protected FocusSession() {
    }

    public FocusSession(Long userId, Clock clock) {
        this.userId = Objects.requireNonNull(userId, "User ID is required");
        this.startedAt = now(clock);
    }

    public void pause(Clock clock) {
        requireNotCompleted();
        if (status == FocusSessionStatus.PAUSED) {
            return;
        }
        Instant instant = now(clock);
        requireValidDuration(instant, totalPausedSeconds);
        pausedAt = instant;
        status = FocusSessionStatus.PAUSED;
    }

    public void resume(Clock clock) {
        requireNotCompleted();
        if (status == FocusSessionStatus.RUNNING) {
            return;
        }
        finishPause(now(clock));
        status = FocusSessionStatus.RUNNING;
    }

    public void complete(Clock clock) {
        if (status == FocusSessionStatus.COMPLETED) {
            return;
        }
        Instant instant = now(clock);
        requireValidDuration(instant, totalPausedSeconds);
        if (status == FocusSessionStatus.PAUSED) {
            finishPause(instant);
        }
        endedAt = instant;
        status = FocusSessionStatus.COMPLETED;
    }

    private void finishPause(Instant instant) {
        if (instant.isBefore(pausedAt)) {
            throw new IllegalArgumentException("A pause cannot end before it started");
        }
        long updatedSeconds = Math.addExact(totalPausedSeconds, Duration.between(pausedAt, instant).getSeconds());
        requireValidDuration(instant, updatedSeconds);
        totalPausedSeconds = updatedSeconds;
        pausedAt = null;
    }

    private void requireValidDuration(Instant instant, long pausedSeconds) {
        if (instant.isBefore(startedAt) || Duration.between(startedAt, instant).getSeconds() < pausedSeconds) {
            throw new IllegalArgumentException("Session timestamps cannot produce a negative active duration");
        }
    }

    private void requireNotCompleted() {
        if (status == FocusSessionStatus.COMPLETED) {
            throw new IllegalStateException("A completed session cannot be restarted");
        }
    }

    private static Instant now(Clock clock) {
        return Objects.requireNonNull(clock, "Clock is required").instant().truncatedTo(ChronoUnit.MICROS);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getTaskId() {
        return taskId;
    }

    // The service must verify that the task belongs to this session's user.
    // Null detaches the task while preserving the session, including after completion.
    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public FocusSessionStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public Instant getPausedAt() {
        return pausedAt;
    }

    public long getTotalPausedSeconds() {
        return totalPausedSeconds;
    }
}
