package com.dsatracker.service;

import com.dsatracker.dto.ContestSessionResponse;
import com.dsatracker.dto.StartContestRequest;
import com.dsatracker.model.ContestProblem;
import com.dsatracker.model.ContestSession;
import com.dsatracker.model.ContestStatus;
import com.dsatracker.model.Difficulty;
import com.dsatracker.model.Problem;
import com.dsatracker.model.Topic;
import com.dsatracker.model.User;
import com.dsatracker.model.UserProgress;
import com.dsatracker.repository.ContestProblemRepository;
import com.dsatracker.repository.ContestSessionRepository;
import com.dsatracker.repository.TopicRepository;
import com.dsatracker.repository.UserProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContestServiceTest {

    @Mock ContestSessionRepository contestSessionRepository;
    @Mock ContestProblemRepository contestProblemRepository;
    @Mock TopicRepository topicRepository;
    @Mock UserProgressRepository userProgressRepository;

    private ContestService contestService;
    private User user;

    @BeforeEach
    void setUp() {
        contestService = new ContestService(contestSessionRepository, contestProblemRepository, topicRepository, userProgressRepository);
        user = new User();
        user.setId(1L);

        // Shared convenience stubs -- not every test below exercises start()/save(), so these
        // are intentionally lenient rather than asserted-used by each individual test.
        lenient().when(contestSessionRepository.save(any(ContestSession.class))).thenAnswer(inv -> {
            ContestSession s = inv.getArgument(0);
            if (s.getId() == null) s.setId(100L);
            return s;
        });
        lenient().when(contestProblemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(userProgressRepository.findAllByUserIdAndStatus(anyLong(), any())).thenReturn(List.of());
    }

    private Problem problem(long id, Difficulty difficulty) {
        Problem p = new Problem();
        p.setId(id);
        p.setTitle("Problem " + id);
        p.setDifficulty(difficulty);
        return p;
    }

    private Topic topicWith(Problem... problems) {
        Topic topic = new Topic();
        topic.setId(1L);
        topic.setProblems(List.of(problems));
        return topic;
    }

    @Test
    void start_respectsDifficultyFilter() {
        Topic topic = topicWith(
                problem(1L, Difficulty.EASY),
                problem(2L, Difficulty.MEDIUM),
                problem(3L, Difficulty.EASY),
                problem(4L, Difficulty.HARD)
        );
        when(topicRepository.findAllByOrderByOrderIndexAsc()).thenReturn(List.of(topic));

        contestService.start(user, new StartContestRequest(10, "EASY", null, 30));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ContestProblem>> captor = ArgumentCaptor.forClass(List.class);
        verify(contestProblemRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue()).allSatisfy(cp -> assertThat(cp.getProblem().getDifficulty()).isEqualTo(Difficulty.EASY));
    }

    @Test
    void start_respectsSheetSlugFilter() {
        Topic topic = topicWith(problem(1L, Difficulty.EASY));
        when(topicRepository.findAllBySheetSlugOrderByOrderIndexAsc("blind75")).thenReturn(List.of(topic));

        contestService.start(user, new StartContestRequest(5, null, "blind75", 30));

        verify(topicRepository).findAllBySheetSlugOrderByOrderIndexAsc("blind75");
        verify(topicRepository, never()).findAllByOrderByOrderIndexAsc();
    }

    @Test
    void start_usesAllSheetsWhenSlugIsAllOrBlank() {
        Topic topic = topicWith(problem(1L, Difficulty.EASY));
        when(topicRepository.findAllByOrderByOrderIndexAsc()).thenReturn(List.of(topic));

        contestService.start(user, new StartContestRequest(5, null, "ALL", 30));

        verify(topicRepository).findAllByOrderByOrderIndexAsc();
        verify(topicRepository, never()).findAllBySheetSlugOrderByOrderIndexAsc(any());
    }

    @Test
    void start_caps_requestedProblemCountAtAvailablePool() {
        Topic topic = topicWith(problem(1L, Difficulty.EASY), problem(2L, Difficulty.EASY));
        when(topicRepository.findAllByOrderByOrderIndexAsc()).thenReturn(List.of(topic));

        contestService.start(user, new StartContestRequest(20, null, null, 30));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ContestProblem>> captor = ArgumentCaptor.forClass(List.class);
        verify(contestProblemRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2); // only 2 problems exist, even though 20 were asked for
    }

    @Test
    void start_clampsDurationToAllowedRange() {
        Topic topic = topicWith(problem(1L, Difficulty.EASY));
        when(topicRepository.findAllByOrderByOrderIndexAsc()).thenReturn(List.of(topic));

        contestService.start(user, new StartContestRequest(1, null, null, 5000));

        ArgumentCaptor<ContestSession> captor = ArgumentCaptor.forClass(ContestSession.class);
        verify(contestSessionRepository).save(captor.capture());
        assertThat(captor.getValue().getDurationMinutes()).isEqualTo(180); // MAX_DURATION_MINUTES
    }

    @Test
    void start_prefersNotYetSolvedProblems() {
        Problem solved = problem(1L, Difficulty.EASY);
        Problem unsolved = problem(2L, Difficulty.EASY);
        Topic topic = topicWith(solved, unsolved);
        when(topicRepository.findAllByOrderByOrderIndexAsc()).thenReturn(List.of(topic));

        UserProgress done = new UserProgress();
        done.setProblem(solved);
        when(userProgressRepository.findAllByUserIdAndStatus(eq(1L), any())).thenReturn(List.of(done));

        contestService.start(user, new StartContestRequest(1, null, null, 30));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ContestProblem>> captor = ArgumentCaptor.forClass(List.class);
        verify(contestProblemRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getProblem().getId()).isEqualTo(2L); // the unsolved one, not the solved one
    }

    @Test
    void markSolvedIfActive_marksAnUnsolvedProblemInAnActiveSession() {
        ContestSession session = new ContestSession();
        session.setId(10L);
        session.setStartedAt(Instant.now());
        session.setDurationMinutes(30); // ends 30 min from now -- still active
        when(contestSessionRepository.findAllByUserIdAndStatus(1L, ContestStatus.IN_PROGRESS)).thenReturn(List.of(session));

        ContestProblem cp = new ContestProblem();
        cp.setSolvedAt(null);
        when(contestProblemRepository.findByContestSessionIdAndProblemId(10L, 5L)).thenReturn(Optional.of(cp));

        contestService.markSolvedIfActive(user, 5L);

        ArgumentCaptor<ContestProblem> captor = ArgumentCaptor.forClass(ContestProblem.class);
        verify(contestProblemRepository).save(captor.capture());
        assertThat(captor.getValue().getSolvedAt()).isNotNull();
    }

    @Test
    void markSolvedIfActive_noopWhenAlreadySolved() {
        ContestSession session = new ContestSession();
        session.setId(10L);
        session.setStartedAt(Instant.now());
        session.setDurationMinutes(30);
        when(contestSessionRepository.findAllByUserIdAndStatus(1L, ContestStatus.IN_PROGRESS)).thenReturn(List.of(session));

        ContestProblem alreadySolved = new ContestProblem();
        alreadySolved.setSolvedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        when(contestProblemRepository.findByContestSessionIdAndProblemId(10L, 5L)).thenReturn(Optional.of(alreadySolved));

        contestService.markSolvedIfActive(user, 5L);

        verify(contestProblemRepository, never()).save(any());
    }

    @Test
    void markSolvedIfActive_noopWhenSessionAlreadyExpired() {
        ContestSession expired = new ContestSession();
        expired.setId(10L);
        expired.setStartedAt(Instant.now().minus(2, ChronoUnit.HOURS));
        expired.setDurationMinutes(30); // ended over an hour ago
        when(contestSessionRepository.findAllByUserIdAndStatus(1L, ContestStatus.IN_PROGRESS)).thenReturn(List.of(expired));

        contestService.markSolvedIfActive(user, 5L);

        verify(contestProblemRepository, never()).findByContestSessionIdAndProblemId(any(), any());
    }

    @Test
    void get_autoExpiresAnOverdueInProgressSession() {
        ContestSession overdue = new ContestSession();
        overdue.setId(10L);
        overdue.setStartedAt(Instant.now().minus(2, ChronoUnit.HOURS));
        overdue.setDurationMinutes(30);
        overdue.setStatus(ContestStatus.IN_PROGRESS);
        when(contestSessionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(overdue));
        when(contestProblemRepository.findAllByContestSessionIdOrderByOrderIndexAsc(10L)).thenReturn(List.of());

        ContestSessionResponse response = contestService.get(user, 10L);

        assertThat(response.status()).isEqualTo(ContestStatus.FINISHED);
        verify(contestSessionRepository, times(1)).save(any(ContestSession.class));
    }

    @Test
    void get_doesNotTouchAStillActiveSession() {
        ContestSession active = new ContestSession();
        active.setId(10L);
        active.setStartedAt(Instant.now());
        active.setDurationMinutes(30);
        active.setStatus(ContestStatus.IN_PROGRESS);
        when(contestSessionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(active));
        when(contestProblemRepository.findAllByContestSessionIdOrderByOrderIndexAsc(10L)).thenReturn(List.of());

        ContestSessionResponse response = contestService.get(user, 10L);

        assertThat(response.status()).isEqualTo(ContestStatus.IN_PROGRESS);
        verify(contestSessionRepository, never()).save(any());
    }
}
