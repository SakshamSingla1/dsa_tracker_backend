package com.dsatracker.repository;

import com.dsatracker.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
    List<Role> findAllByOrderByNameAsc();
    boolean existsByName(String name);
}
