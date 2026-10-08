package com.dsatracker.repository;

import com.dsatracker.model.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {
    List<InterviewSession> findAllByUserIdOrderByStartedAtDesc(Long userId);

    Optional<InterviewSession> findByIdAndUserId(Long id, Long userId);
}
