package com.dsatracker.service;

import com.dsatracker.dto.ProblemResponse;
import com.dsatracker.model.Problem;
import com.dsatracker.model.Status;
import com.dsatracker.model.UserProgress;
import com.dsatracker.repository.UserProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock UserProgressRepository userProgressRepository;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(userProgressRepository);
    }

    private UserProgress reviseRow(long problemId, LocalDate dueAt) {
        UserProgress up = new UserProgress();
        Problem p = new Problem();
        p.setId(problemId);
        up.setProblem(p);
        up.setStatus(Status.REVISE);
        up.setReviewDueAt(dueAt);
        return up;
    }

    @Test
    void getDueQueue_onlyReturnsProblemsDueTodayOrEarlier_soonestFirst() {
        UserProgress overdue = reviseRow(1L, LocalDate.now().minusDays(2));
        UserProgress dueToday = reviseRow(2L, LocalDate.now());
        UserProgress notYetDue = reviseRow(3L, LocalDate.now().plusDays(1));
        when(userProgressRepository.findAllByUserIdAndStatus(1L, Status.REVISE))
                .thenReturn(List.of(dueToday, notYetDue, overdue)); // deliberately out of order

        List<ProblemResponse> queue = reviewService.getDueQueue(1L);

        assertThat(queue).hasSize(2);
        assertThat(queue.get(0).id()).isEqualTo(1L); // most overdue first
        assertThat(queue.get(1).id()).isEqualTo(2L);
    }

    @Test
    void getDueQueue_excludesRowsWithNoDueDateSet() {
        UserProgress noDueDate = reviseRow(1L, null);
        when(userProgressRepository.findAllByUserIdAndStatus(1L, Status.REVISE)).thenReturn(List.of(noDueDate));

        List<ProblemResponse> queue = reviewService.getDueQueue(1L);

        assertThat(queue).isEmpty();
    }

    @Test
    void getDueQueue_emptyWhenNothingInRevise() {
        when(userProgressRepository.findAllByUserIdAndStatus(1L, Status.REVISE)).thenReturn(List.of());

        assertThat(reviewService.getDueQueue(1L)).isEmpty();
    }
}
