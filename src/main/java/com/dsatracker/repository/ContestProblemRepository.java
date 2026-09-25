package com.dsatracker.repository;

import com.dsatracker.model.ContestProblem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContestProblemRepository extends JpaRepository<ContestProblem, Long> {
    List<ContestProblem> findAllByContestSessionIdOrderByOrderIndexAsc(Long contestSessionId);
    Optional<ContestProblem> findByContestSessionIdAndProblemId(Long contestSessionId, Long problemId);

    void deleteAllByContestSession_User_Id(Long userId);
}
