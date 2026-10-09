package com.dsatracker.repository;

import com.dsatracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    /** Guards admin role deletion -- a role still assigned to a user can't be deleted. */
    boolean existsByRoleId(Long roleId);

    long countByRoleIsNotNullAndEnabledTrue();
}
