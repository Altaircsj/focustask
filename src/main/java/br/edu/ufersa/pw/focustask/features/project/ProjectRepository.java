package br.edu.ufersa.pw.focustask.features.project;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

interface ProjectRepository extends JpaRepository<Project, Long> {
    @EntityGraph(attributePaths = "tags")
    List<Project> findAllByUserId(Long userId);
    Optional<Project> findByIdAndUserId(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Project p where p.id = :id and p.userId = :userId")
    Optional<Project> findForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    @Transactional(propagation = Propagation.MANDATORY)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Project p where p.userId = :userId")
    int deleteAllForUser(@Param("userId") Long userId);
}
