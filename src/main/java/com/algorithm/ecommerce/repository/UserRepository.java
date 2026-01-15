package com.algorithm.ecommerce.repository;

import com.algorithm.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);

    // NUEVO: Para validar registro duplicado
    boolean existsByUsername(String username);
}