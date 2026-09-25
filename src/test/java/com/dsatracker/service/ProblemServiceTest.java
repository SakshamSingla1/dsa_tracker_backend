package com.dsatracker.service;

import com.dsatracker.dto.ProblemResponse;
import com.dsatracker.model.Problem;
import com.dsatracker.model.ReviewOutcome;
import com.dsatracker.model.Status;
import com.dsatracker.model.User;
import com.dsatracker.model.UserProgress;
import com.dsatracker.repository.ProblemRepository;
import com.dsatracker.repository.UserProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/** Leitner-ladder spaced-repetition logic: REVIEW_INTERVALS = {1, 3, 7, 14, 30} days. */
@ExtendWith(MockitoExtension.class)
class ProblemServiceTest {

    @Mock ProblemRepository problemRepository;
    @Mock UserProgressRepository userProgressRepository;

    private ProblemService problemService;
    private User user;

    @BeforeEach
    void setUp() {
        problemService = new ProblemService(problemRepository, userProgressRepository);
        user = new User();
        user.setId(1L);
        lenient().when(userProgressRepository.save(any(UserProgress.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private UserProgress reviseProgressAtStage(int stage) {
        UserProgress progress = new UserProgress();
        progress.setUser(user);
        progress.setProblem(new Problem());
        progress.setStatus(Status.REVISE);
        progress.setReviewStage(stage);
        when(userProgressRepository.findByUserIdAndProblemId(1L, 10L)).thenReturn(Optional.of(progress));
        return progress;
    }

    @Test
    void review_rememberedAdvancesStageAndReschedulesToTheNextInterval() {
        reviseProgressAtStage(0); // interval[0]=1 day -> remembered should move to stage 1 (interval[1]=3 days)

        ProblemResponse response = problemService.review(user, 10L, ReviewOutcome.REMEMBERED);

        assertThat(response.reviewDueAt()).isEqualTo(LocalDate.now().plusDays(3));
    }

    @Test
    void review_rememberedCapsAtTheLastRungOfTheLadder() {
        reviseProgressAtStage(4); // already at the last interval (30 days) -- remembered again must not go out of bounds

        ProblemResponse response = problemService.review(user, 10L, ReviewOutcome.REMEMBERED);

        assertThat(response.reviewDueAt()).isEqualTo(LocalDate.now().plusDays(30));
    }

    @Test
    void review_forgotResetsToStageZero() {
        reviseProgressAtStage(3); // interval[3]=14 days

        ProblemResponse response = problemService.review(user, 10L, ReviewOutcome.FORGOT);

        assertThat(response.reviewDueAt()).isEqualTo(LocalDate.now().plusDays(1)); // back to interval[0]
    }

    @Test
    void review_rejectsWhenProblemIsNotCurrentlyMarkedRevise() {
        UserProgress done = new UserProgress();
        done.setStatus(Status.DONE);
        when(userProgressRepository.findByUserIdAndProblemId(1L, 10L)).thenReturn(Optional.of(done));

        assertThatThrownBy(() -> problemService.review(user, 10L, ReviewOutcome.REMEMBERED))
                .hasMessageContaining("not marked for review");
    }

    @Test
    void review_rejectsWhenNoProgressRowExists() {
        when(userProgressRepository.findByUserIdAndProblemId(1L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> problemService.review(user, 10L, ReviewOutcome.REMEMBERED))
                .hasMessageContaining("No progress to review");
    }

    @Test
    void update_enteringReviseResetsStageAndSchedulesOneDayOut() {
        UserProgress fresh = new UserProgress();
        fresh.setUser(user);
        Problem problem = new Problem();
        problem.setId(10L);
        fresh.setProblem(problem);
        fresh.setStatus(Status.TODO);
        when(problemRepository.findById(10L)).thenReturn(Optional.of(problem));
        when(userProgressRepository.findByUserIdAndProblemId(1L, 10L)).thenReturn(Optional.of(fresh));

        ProblemResponse response = problemService.update(user, 10L,
                new com.dsatracker.dto.ProblemUpdateRequest(Status.REVISE, null, null, null));

        assertThat(response.status()).isEqualTo(Status.REVISE);
        assertThat(response.reviewDueAt()).isEqualTo(LocalDate.now().plusDays(1));
    }

    @Test
    void update_leavingReviseClearsTheDueDate() {
        UserProgress inRevise = new UserProgress();
        inRevise.setUser(user);
        Problem problem = new Problem();
        problem.setId(10L);
        inRevise.setProblem(problem);
        inRevise.setStatus(Status.REVISE);
        inRevise.setReviewStage(2);
        inRevise.setReviewDueAt(LocalDate.now().plusDays(7));
        when(problemRepository.findById(10L)).thenReturn(Optional.of(problem));
        when(userProgressRepository.findByUserIdAndProblemId(1L, 10L)).thenReturn(Optional.of(inRevise));

        ProblemResponse response = problemService.update(user, 10L,
                new com.dsatracker.dto.ProblemUpdateRequest(Status.DONE, null, null, null));

        assertThat(response.status()).isEqualTo(Status.DONE);
        assertThat(response.reviewDueAt()).isNull();
    }
}
