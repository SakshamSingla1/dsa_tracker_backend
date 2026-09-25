package com.dsatracker.service;

import com.dsatracker.dto.JudgeRequest;
import com.dsatracker.dto.JudgeResponse;
import com.dsatracker.dto.TestCaseResult;
import com.dsatracker.model.Problem;
import com.dsatracker.model.Status;
import com.dsatracker.model.Submission;
import com.dsatracker.model.TestCase;
import com.dsatracker.model.User;
import com.dsatracker.model.UserProgress;
import com.dsatracker.model.Verdict;
import com.dsatracker.repository.ProblemRepository;
import com.dsatracker.repository.SubmissionRepository;
import com.dsatracker.repository.UserProgressRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Judges code against a problem's test cases, the way an online judge does:
 * "Run" checks only the sample cases and doesn't persist anything; "Submit"
 * checks every case, records a {@link Submission}, and marks the problem
 * DONE for the user on an ACCEPTED verdict.
 */
@Service
public class JudgeService {

    private final ProblemRepository problemRepository;
    private final CodeRunnerService codeRunnerService;
    private final SubmissionRepository submissionRepository;
    private final UserProgressRepository userProgressRepository;
    private final ContestService contestService;

    public JudgeService(
            ProblemRepository problemRepository,
            CodeRunnerService codeRunnerService,
            SubmissionRepository submissionRepository,
            UserProgressRepository userProgressRepository,
            ContestService contestService
    ) {
        this.problemRepository = problemRepository;
        this.codeRunnerService = codeRunnerService;
        this.submissionRepository = submissionRepository;
        this.userProgressRepository = userProgressRepository;
        this.contestService = contestService;
    }

    @Transactional(readOnly = true)
    public JudgeResponse run(Long problemId, JudgeRequest request) {
        Problem problem = findProblem(problemId);
        List<TestCase> samples = problem.getTestCases().stream().filter(TestCase::isSample).toList();
        return judge(problem, request, samples);
    }

    @Transactional
    public JudgeResponse submit(User user, Long problemId, JudgeRequest request) {
        Problem problem = findProblem(problemId);
        List<TestCase> all = problem.getTestCases();
        JudgeResponse result = judge(problem, request, all);

        Submission submission = new Submission();
        submission.setUser(user);
        submission.setProblem(problem);
        submission.setLanguage(request.language());
        submission.setCode(request.code());
        submission.setVerdict(result.verdict());
        submission.setPassedCount(result.passedCount());
        submission.setTotalCount(result.totalCount());
        submission.setRuntimeMs(result.durationMs());
        submission = submissionRepository.save(submission);

        if (result.verdict() == Verdict.ACCEPTED) {
            UserProgress progress = userProgressRepository.findByUserIdAndProblemId(user.getId(), problemId)
                    .orElseGet(() -> {
                        UserProgress p = new UserProgress();
                        p.setUser(user);
                        p.setProblem(problem);
                        return p;
                    });
            if (progress.getStatus() != Status.DONE) {
                progress.setCompletedAt(LocalDate.now());
            }
            progress.setStatus(Status.DONE);
            userProgressRepository.save(progress);

            if (request.contestSessionId() != null) {
                contestService.markSolvedIfActive(user, problemId);
            }
        }

        return new JudgeResponse(
                result.verdict(), result.compileError(), result.passedCount(), result.totalCount(),
                result.durationMs(), result.results(), submission.getId()
        );
    }

    private JudgeResponse judge(Problem problem, JudgeRequest request, List<TestCase> cases) {
        if (cases.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This problem has no test cases yet.");
        }

        long start = System.currentTimeMillis();
        CodeRunnerService.BatchRunResult batch = codeRunnerService.runBatch(
                request.language(), request.code(), cases.stream().map(TestCase::getInput).toList()
        );
        long duration = System.currentTimeMillis() - start;

        if (!batch.compiled()) {
            return new JudgeResponse(Verdict.COMPILE_ERROR, batch.compileError(), 0, cases.size(), duration, List.of(), null);
        }

        List<TestCaseResult> results = new ArrayList<>();
        boolean anyTimedOut = false;
        boolean anyRuntimeError = false;
        int passedCount = 0;

        for (int i = 0; i < cases.size(); i++) {
            TestCase tc = cases.get(i);
            CodeRunnerService.CaseRun run = batch.cases().get(i);
            boolean crashed = !run.timedOut() && run.exitCode() != null && run.exitCode() != 0;
            boolean passed = !run.timedOut() && !crashed && outputsMatch(tc.getExpectedOutput(), run.stdout());

            if (run.timedOut()) anyTimedOut = true;
            else if (crashed) anyRuntimeError = true;
            if (passed) passedCount++;

            results.add(new TestCaseResult(
                    i + 1,
                    tc.isSample(),
                    tc.isSample() ? tc.getInput() : null,
                    tc.isSample() ? tc.getExpectedOutput() : null,
                    tc.isSample() ? run.stdout() : null,
                    passed,
                    run.timedOut(),
                    run.stderr()
            ));
        }

        Verdict verdict;
        if (anyTimedOut) verdict = Verdict.TIME_LIMIT_EXCEEDED;
        else if (anyRuntimeError) verdict = Verdict.RUNTIME_ERROR;
        else if (passedCount == cases.size()) verdict = Verdict.ACCEPTED;
        else verdict = Verdict.WRONG_ANSWER;

        return new JudgeResponse(verdict, null, passedCount, cases.size(), duration, results, null);
    }

    /** Compares two outputs ignoring trailing whitespace per line and trailing blank lines. */
    private boolean outputsMatch(String expected, String actual) {
        return normalize(expected).equals(normalize(actual));
    }

    private String normalize(String s) {
        if (s == null) s = "";
        String[] lines = s.replace("\r\n", "\n").split("\n", -1);
        int end = lines.length;
        while (end > 0 && lines[end - 1].stripTrailing().isEmpty()) end--;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < end; i++) {
            if (i > 0) sb.append('\n');
            sb.append(lines[i].stripTrailing());
        }
        return sb.toString();
    }

    private Problem findProblem(Long problemId) {
        return problemRepository.findById(problemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No problem with id " + problemId));
    }
}
