package com.dsatracker.repository;

import com.dsatracker.model.InterviewMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewMessageRepository extends JpaRepository<InterviewMessage, Long> {
    List<InterviewMessage> findAllBySessionIdOrderByCreatedAtAsc(Long sessionId);
}
