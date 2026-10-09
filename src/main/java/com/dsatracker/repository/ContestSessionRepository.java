package com.dsatracker.repository;

import com.dsatracker.model.ContestSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContestSessionRepository extends JpaRepository<ContestSession, Long> {
    Optional<ContestSession> findByIdAndUserId(Long id, Long userId);
    List<ContestSession> findAllByUserIdOrderByStartedAtDesc(Long userId);

    void deleteAllByUserId(Long userId);
}
