package br.edu.ufersa.pw.focustask.features.focusSession;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Expects User.id, Project.id and Project.user to be mapped by their features.
 * Bulk operations must run inside the deletion service's transaction.
 */
public interface FocusSessionRepository extends JpaRepository<FocusSession, Long> {

    List<FocusSession> findAllByUser_Id(Long userId);

    Optional<FocusSession> findByIdAndUser_Id(Long id, Long userId);

    List<FocusSession> findAllByTask_IdAndUser_Id(Long taskId, Long userId);

    @Transactional(propagation = Propagation.MANDATORY)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update FocusSession s set s.task = null
            where s.user.id = :userId and s.task.id in (
                select t.id from Task t where t.id = :taskId and t.project.user.id = :userId
            )
            """)
    int detachFromTask(@Param("userId") Long userId, @Param("taskId") Long taskId);

    @Transactional(propagation = Propagation.MANDATORY)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update FocusSession s set s.task = null
            where s.user.id = :userId and s.task.id in (
                select t.id from Task t
                where t.project.id = :projectId and t.project.user.id = :userId
            )
            """)
    int detachFromProject(@Param("userId") Long userId, @Param("projectId") Long projectId);

    @Transactional(propagation = Propagation.MANDATORY)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from FocusSession s where s.user.id = :userId")
    int deleteAllForUser(@Param("userId") Long userId);
}
