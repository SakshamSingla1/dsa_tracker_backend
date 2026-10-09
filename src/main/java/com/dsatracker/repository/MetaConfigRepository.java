package com.dsatracker.repository;

import com.dsatracker.model.MetaConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetaConfigRepository extends JpaRepository<MetaConfig, Long> {
    Optional<MetaConfig> findByContext(String context);
    List<MetaConfig> findAllByOrderByContextAsc();
    boolean existsByContext(String context);
}
