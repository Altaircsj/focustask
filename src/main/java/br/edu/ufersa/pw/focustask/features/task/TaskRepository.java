package br.edu.ufersa.pw.focustask.features.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** Deletion services must detach focus sessions before deleting tasks. */
interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByProjectId(Long projectId);
    List<Task> findAllByProjectIdIn(List<Long> projectIds);

    @Transactional(propagation = Propagation.MANDATORY)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Task t where t.projectId in :projectIds")
    int deleteAllForProjects(@Param("projectIds") List<Long> projectIds);
}
