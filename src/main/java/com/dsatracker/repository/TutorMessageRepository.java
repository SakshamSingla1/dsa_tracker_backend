package com.dsatracker.repository;

import com.dsatracker.model.TutorMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TutorMessageRepository extends JpaRepository<TutorMessage, Long> {
    List<TutorMessage> findAllByUserIdAndProblemIdOrderByCreatedAtAsc(Long userId, Long problemId);

    void deleteAllByUserIdAndProblemId(Long userId, Long problemId);
}
