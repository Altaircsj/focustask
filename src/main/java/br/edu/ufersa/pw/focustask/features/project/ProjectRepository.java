package br.edu.ufersa.pw.focustask.features.project;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
interface ProjectRepository extends JpaRepository<Project, Long> {

    // Agora busca por userId, em vez de user.id
    List<Project> findAllByUserId(Long userId);

    Optional<Project> findByIdAndUserId(Long id, Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Project p WHERE p.userId = :userId")
    int deleteAllForUser(@Param("userId") Long userId);
}