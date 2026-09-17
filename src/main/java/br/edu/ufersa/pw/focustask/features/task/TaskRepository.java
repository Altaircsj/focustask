package br.edu.ufersa.pw.focustask.features.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Expects Project.id and Project.user.id to be mapped by the project feature.
 * Deletions require the service to detach focus sessions first, in the same transaction.
 * Inherited delete methods do not perform that detachment automatically.
 */
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByProject_User_Id(Long userId);

    Optional<Task> findByIdAndProject_User_Id(Long id, Long userId);

    List<Task> findAllByProject_IdAndProject_User_Id(Long projectId, Long userId);

    @Transactional(propagation = Propagation.MANDATORY)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from Task t
            where t.project.id in (
                select p.id from Project p where p.id = :projectId and p.user.id = :userId
            )
            """)
    int deleteAllForProject(@Param("userId") Long userId, @Param("projectId") Long projectId);

    @Transactional(propagation = Propagation.MANDATORY)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from Task t
            where t.project.id in (select p.id from Project p where p.user.id = :userId)
            """)
    int deleteAllForUser(@Param("userId") Long userId);
}
