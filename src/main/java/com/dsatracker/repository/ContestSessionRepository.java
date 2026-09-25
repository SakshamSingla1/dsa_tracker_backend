package com.dsatracker.repository;

import com.dsatracker.model.ContestSession;
import com.dsatracker.model.ContestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContestSessionRepository extends JpaRepository<ContestSession, Long> {
    Optional<ContestSession> findByIdAndUserId(Long id, Long userId);
    List<ContestSession> findAllByUserIdOrderByStartedAtDesc(Long userId);
    List<ContestSession> findAllByUserIdAndStatus(Long userId, ContestStatus status);

    void deleteAllByUserId(Long userId);
}
