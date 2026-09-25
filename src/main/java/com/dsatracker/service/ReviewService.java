package com.dsatracker.service;

import com.dsatracker.dto.ProblemResponse;
import com.dsatracker.model.Status;
import com.dsatracker.model.UserProgress;
import com.dsatracker.repository.UserProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class ReviewService {

    private final UserProgressRepository userProgressRepository;

    public ReviewService(UserProgressRepository userProgressRepository) {
        this.userProgressRepository = userProgressRepository;
    }

    /** Every REVISE problem, across all sheets, whose review is due today or earlier -- soonest first. */
    @Transactional(readOnly = true)
    public List<ProblemResponse> getDueQueue(Long userId) {
        LocalDate today = LocalDate.now();
        return userProgressRepository.findAllByUserIdAndStatus(userId, Status.REVISE).stream()
                .filter(up -> up.getReviewDueAt() != null && !up.getReviewDueAt().isAfter(today))
                .sorted(Comparator.comparing(UserProgress::getReviewDueAt))
                .map(up -> ProblemResponse.from(up.getProblem(), up))
                .toList();
    }
}
