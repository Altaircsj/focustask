package br.edu.ufersa.pw.focustask.features.focusSession;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

interface FocusSessionRepository extends JpaRepository<FocusSession, Long> {
    List<FocusSession> findAllByUserId(Long userId);
    Optional<FocusSession> findByIdAndUserId(Long id, Long userId);
    List<FocusSession> findAllByTaskIdAndUserId(Long taskId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from FocusSession s where s.id = :id and s.userId = :userId")
    Optional<FocusSession> findForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    @Transactional(propagation = Propagation.MANDATORY)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update FocusSession s set s.taskId = null where s.userId = :userId and s.taskId in :taskIds")
    int detachFromTasks(@Param("userId") Long userId, @Param("taskIds") List<Long> taskIds);

    @Transactional(propagation = Propagation.MANDATORY)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from FocusSession s where s.userId = :userId")
    int deleteAllForUser(@Param("userId") Long userId);
}
