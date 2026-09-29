package br.edu.ufersa.pw.focustask.features.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface UserRepository extends JpaRepository<User, Long> {
    java.util.Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
}