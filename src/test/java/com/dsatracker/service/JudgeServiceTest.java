package com.dsatracker.service;

import com.dsatracker.dto.JudgeRequest;
import com.dsatracker.dto.JudgeResponse;
import com.dsatracker.model.Language;
import com.dsatracker.model.Problem;
import com.dsatracker.model.TestCase;
import com.dsatracker.model.User;
import com.dsatracker.model.Verdict;
import com.dsatracker.repository.ProblemRepository;
import com.dsatracker.repository.SubmissionRepository;
import com.dsatracker.repository.UserProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verdict-determination logic is tested via {@code run()} (no persistence side effects) with
 * {@link CodeRunnerService} mocked out -- these tests are about JudgeService's own branching
 * (compiled/timeout/crash/output-match -> verdict), not about actually compiling/running code.
 */
@ExtendWith(MockitoExtension.class)
class JudgeServiceTest {

    @Mock ProblemRepository problemRepository;
    @Mock CodeRunnerService codeRunnerService;
    @Mock SubmissionRepository submissionRepository;
    @Mock UserProgressRepository userProgressRepository;
    @Mock ContestService contestService;

    private JudgeService judgeService;

    @BeforeEach
    void setUp() {
        judgeService = new JudgeService(problemRepository, codeRunnerService, submissionRepository, userProgressRepository, contestService);
    }

    private Problem problemWithOneSample(String input, String expectedOutput) {
        Problem problem = new Problem();
        problem.setId(1L);
        TestCase tc = new TestCase();
        tc.setInput(input);
        tc.setExpectedOutput(expectedOutput);
        tc.setSample(true);
        problem.setTestCases(new java.util.ArrayList<>(List.of(tc)));
        return problem;
    }

    private JudgeRequest request() {
        return new JudgeRequest(Language.JAVA, "irrelevant source", null);
    }

    @Test
    void run_acceptedWhenOutputMatchesExactly() {
        Problem problem = problemWithOneSample("5", "25");
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        when(codeRunnerService.runBatch(eq(Language.JAVA), any(), anyList())).thenReturn(
                new CodeRunnerService.BatchRunResult(true, null, List.of(
                        new CodeRunnerService.CaseRun("25\n", "", 0, false, 12)
                ))
        );

        JudgeResponse response = judgeService.run(1L, request());

        assertThat(response.verdict()).isEqualTo(Verdict.ACCEPTED);
        assertThat(response.passedCount()).isEqualTo(1);
    }

    @Test
    void run_outputsMatch_ignoresTrailingWhitespaceAndBlankLines() {
        Problem problem = problemWithOneSample("5", "25");
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        when(codeRunnerService.runBatch(any(), any(), anyList())).thenReturn(
                new CodeRunnerService.BatchRunResult(true, null, List.of(
                        // trailing spaces on the line + trailing blank lines -- should still match "25"
                        new CodeRunnerService.CaseRun("25   \n\n\n", "", 0, false, 8)
                ))
        );

        JudgeResponse response = judgeService.run(1L, request());

        assertThat(response.verdict()).isEqualTo(Verdict.ACCEPTED);
    }

    @Test
    void run_wrongAnswerWhenOutputDiffers() {
        Problem problem = problemWithOneSample("5", "25");
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        when(codeRunnerService.runBatch(any(), any(), anyList())).thenReturn(
                new CodeRunnerService.BatchRunResult(true, null, List.of(
                        new CodeRunnerService.CaseRun("26\n", "", 0, false, 10)
                ))
        );

        JudgeResponse response = judgeService.run(1L, request());

        assertThat(response.verdict()).isEqualTo(Verdict.WRONG_ANSWER);
        assertThat(response.passedCount()).isEqualTo(0);
    }

    @Test
    void run_compileErrorWhenBatchDidNotCompile() {
        Problem problem = problemWithOneSample("5", "25");
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        when(codeRunnerService.runBatch(any(), any(), anyList())).thenReturn(
                CodeRunnerService.BatchRunResult.compileFailure("cannot find symbol")
        );

        JudgeResponse response = judgeService.run(1L, request());

        assertThat(response.verdict()).isEqualTo(Verdict.COMPILE_ERROR);
        assertThat(response.compileError()).isEqualTo("cannot find symbol");
    }

    @Test
    void run_timeLimitExceededWhenCaseTimesOut() {
        Problem problem = problemWithOneSample("5", "25");
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        when(codeRunnerService.runBatch(any(), any(), anyList())).thenReturn(
                new CodeRunnerService.BatchRunResult(true, null, List.of(
                        new CodeRunnerService.CaseRun("", "", null, true, 8000)
                ))
        );

        JudgeResponse response = judgeService.run(1L, request());

        assertThat(response.verdict()).isEqualTo(Verdict.TIME_LIMIT_EXCEEDED);
    }

    @Test
    void run_runtimeErrorWhenProcessExitsNonZero() {
        Problem problem = problemWithOneSample("5", "25");
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        when(codeRunnerService.runBatch(any(), any(), anyList())).thenReturn(
                new CodeRunnerService.BatchRunResult(true, null, List.of(
                        new CodeRunnerService.CaseRun("", "Exception in thread main", 1, false, 5)
                ))
        );

        JudgeResponse response = judgeService.run(1L, request());

        assertThat(response.verdict()).isEqualTo(Verdict.RUNTIME_ERROR);
    }

    @Test
    void submit_marksContestProblemSolvedWhenAcceptedWithContestSessionId() {
        Problem problem = problemWithOneSample("5", "25");
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        when(codeRunnerService.runBatch(any(), any(), anyList())).thenReturn(
                new CodeRunnerService.BatchRunResult(true, null, List.of(
                        new CodeRunnerService.CaseRun("25\n", "", 0, false, 10)
                ))
        );
        when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userProgressRepository.findByUserIdAndProblemId(any(), any())).thenReturn(Optional.empty());

        User user = new User();
        user.setId(9L);

        judgeService.submit(user, 1L, new JudgeRequest(Language.JAVA, "code", 55L));

        verify(contestService).markSolvedIfActive(user, 1L);
    }

    @Test
    void submit_doesNotTouchContestWhenNoContestSessionId() {
        Problem problem = problemWithOneSample("5", "25");
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        when(codeRunnerService.runBatch(any(), any(), anyList())).thenReturn(
                new CodeRunnerService.BatchRunResult(true, null, List.of(
                        new CodeRunnerService.CaseRun("25\n", "", 0, false, 10)
                ))
        );
        when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userProgressRepository.findByUserIdAndProblemId(any(), any())).thenReturn(Optional.empty());

        User user = new User();
        user.setId(9L);

        judgeService.submit(user, 1L, new JudgeRequest(Language.JAVA, "code", null));

        verify(contestService, never()).markSolvedIfActive(any(), any());
    }

    @Test
    void submit_doesNotTouchContestWhenVerdictIsNotAccepted() {
        Problem problem = problemWithOneSample("5", "25");
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        when(codeRunnerService.runBatch(any(), any(), anyList())).thenReturn(
                new CodeRunnerService.BatchRunResult(true, null, List.of(
                        new CodeRunnerService.CaseRun("26\n", "", 0, false, 10) // wrong answer
                ))
        );
        when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User user = new User();
        user.setId(9L);

        judgeService.submit(user, 1L, new JudgeRequest(Language.JAVA, "code", 55L));

        verify(contestService, never()).markSolvedIfActive(any(), any());
        verify(userProgressRepository, never()).save(any());
    }
}
