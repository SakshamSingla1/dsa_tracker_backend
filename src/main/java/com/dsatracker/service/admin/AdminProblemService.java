package com.dsatracker.service.admin;

import com.dsatracker.dto.admin.AdminExampleDto;
import com.dsatracker.dto.admin.AdminProblemRequest;
import com.dsatracker.dto.admin.AdminProblemResponse;
import com.dsatracker.dto.admin.AdminProblemSummaryResponse;
import com.dsatracker.dto.admin.AdminTestCaseDto;
import com.dsatracker.model.Problem;
import com.dsatracker.model.TestCase;
import com.dsatracker.model.Topic;
import com.dsatracker.repository.CommentRepository;
import com.dsatracker.repository.ProblemRepository;
import com.dsatracker.repository.SubmissionRepository;
import com.dsatracker.repository.TopicRepository;
import com.dsatracker.repository.UserProgressRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminProblemService {

    private final ProblemRepository problemRepository;
    private final TopicRepository topicRepository;
    private final UserProgressRepository userProgressRepository;
    private final SubmissionRepository submissionRepository;
    private final CommentRepository commentRepository;

    public AdminProblemService(
            ProblemRepository problemRepository,
            TopicRepository topicRepository,
            UserProgressRepository userProgressRepository,
            SubmissionRepository submissionRepository,
            CommentRepository commentRepository
    ) {
        this.problemRepository = problemRepository;
        this.topicRepository = topicRepository;
        this.userProgressRepository = userProgressRepository;
        this.submissionRepository = submissionRepository;
        this.commentRepository = commentRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminProblemSummaryResponse> listProblems() {
        return problemRepository.findAll().stream().map(AdminProblemSummaryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AdminProblemResponse getProblem(Long id) {
        return AdminProblemResponse.from(findProblem(id));
    }

    @Transactional
    public AdminProblemResponse createProblem(AdminProblemRequest request) {
        validate(request);
        Problem problem = new Problem();
        problem.setTopic(findTopic(request.topicId()));
        applyRequest(problem, request);
        return AdminProblemResponse.from(problemRepository.save(problem));
    }

    @Transactional
    public AdminProblemResponse updateProblem(Long id, AdminProblemRequest request) {
        validate(request);
        Problem problem = findProblem(id);
        problem.setTopic(findTopic(request.topicId()));
        applyRequest(problem, request);
        return AdminProblemResponse.from(problemRepository.save(problem));
    }

    @Transactional
    public void deleteProblem(Long id) {
        Problem problem = findProblem(id);
        if (userProgressRepository.existsByProblemId(id) || submissionRepository.existsByProblemId(id) || commentRepository.existsByProblemId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This problem has recorded progress, submissions, or comments and can't be deleted.");
        }
        problemRepository.delete(problem);
    }

    private void validate(AdminProblemRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required.");
        }
        if (request.difficulty() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Difficulty is required.");
        }
        if (request.topicId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "topicId is required.");
        }
    }

    private void applyRequest(Problem problem, AdminProblemRequest request) {
        problem.setTitle(request.title().trim());
        problem.setDifficulty(request.difficulty());
        problem.setStatement(request.statement());
        problem.setTimeComplexity(request.timeComplexity());
        problem.setSpaceComplexity(request.spaceComplexity());
        problem.setEditorialUrl(blankToNull(request.editorialUrl()));
        problem.setVideoUrl(blankToNull(request.videoUrl()));
        problem.setExternalUrl(blankToNull(request.externalUrl()));
        problem.setOrderIndex(request.orderIndex());
        problem.setConstraints(request.constraints() != null ? request.constraints() : List.of());
        problem.setHints(request.hints() != null ? request.hints() : List.of());
        problem.setTags(request.tags() != null ? request.tags() : List.of());
        problem.setExamples(
                request.examples() == null ? List.of() : request.examples().stream().map(AdminExampleDto::toEntity).toList()
        );
        reconcileTestCases(problem, request.testCases() != null ? request.testCases() : List.of());
    }

    /** Updates existing test cases in place (matched by id), adds new ones (no id), and removes
     *  any existing one not present in the incoming list -- orphanRemoval deletes it from the DB. */
    private void reconcileTestCases(Problem problem, List<AdminTestCaseDto> incoming) {
        Map<Long, TestCase> existingById = new HashMap<>();
        for (TestCase tc : problem.getTestCases()) {
            existingById.put(tc.getId(), tc);
        }

        List<TestCase> result = new ArrayList<>();
        for (int i = 0; i < incoming.size(); i++) {
            AdminTestCaseDto dto = incoming.get(i);
            TestCase testCase = dto.id() != null ? existingById.remove(dto.id()) : null;
            if (testCase == null) {
                testCase = new TestCase();
                testCase.setProblem(problem);
            }
            testCase.setInput(dto.input());
            testCase.setExpectedOutput(dto.expectedOutput());
            testCase.setSample(dto.sample());
            testCase.setOrderIndex(i);
            result.add(testCase);
        }

        problem.getTestCases().clear();
        problem.getTestCases().addAll(result);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private Problem findProblem(Long id) {
        return problemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No problem with id " + id));
    }

    private Topic findTopic(Long topicId) {
        return topicRepository.findById(topicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No topic with id " + topicId));
    }
}
