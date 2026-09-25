package com.dsatracker.service;

import com.dsatracker.dto.ProblemOfDayResponse;
import com.dsatracker.dto.ProblemResponse;
import com.dsatracker.dto.ProblemUpdateRequest;
import com.dsatracker.model.Problem;
import com.dsatracker.model.ReviewOutcome;
import com.dsatracker.model.Status;
import com.dsatracker.model.Topic;
import com.dsatracker.model.User;
import com.dsatracker.model.UserProgress;
import com.dsatracker.repository.ProblemRepository;
import com.dsatracker.repository.UserProgressRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
public class ProblemService {

    /** Leitner-style spaced-repetition ladder, in days -- index by UserProgress.reviewStage. */
    private static final int[] REVIEW_INTERVALS = {1, 3, 7, 14, 30};

    private final ProblemRepository problemRepository;
    private final UserProgressRepository userProgressRepository;

    public ProblemService(ProblemRepository problemRepository, UserProgressRepository userProgressRepository) {
        this.problemRepository = problemRepository;
        this.userProgressRepository = userProgressRepository;
    }

    /** Fetches a single problem by id regardless of which sheet is currently active client-side --
     *  used when jumping into a problem from a cross-sheet context (e.g. a contest). */
    @Transactional(readOnly = true)
    public ProblemOfDayResponse getById(User currentUser, Long problemId) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No problem with id " + problemId));
        UserProgress progress = userProgressRepository.findByUserIdAndProblemId(currentUser.getId(), problemId).orElse(null);
        Topic topic = problem.getTopic();
        return new ProblemOfDayResponse(
                ProblemResponse.from(problem, progress),
                topic != null ? topic.getName() : null,
                topic != null ? topic.getId() : null
        );
    }

    @Transactional
    public ProblemResponse update(User currentUser, Long problemId, ProblemUpdateRequest request) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No problem with id " + problemId));

        // externalUrl is shared reference data -- everyone benefits from a fixed link.
        if (request.externalUrl() != null) {
            problem.setExternalUrl(request.externalUrl().isBlank() ? null : request.externalUrl());
            problem = problemRepository.save(problem);
        }

        Problem savedProblem = problem;
        UserProgress progress = null;
        if (request.status() != null || request.notes() != null || request.bookmarked() != null) {
            progress = userProgressRepository.findByUserIdAndProblemId(currentUser.getId(), problemId)
                    .orElseGet(() -> {
                        UserProgress p = new UserProgress();
                        p.setUser(currentUser);
                        p.setProblem(savedProblem);
                        return p;
                    });

            if (request.status() != null) {
                Status previousStatus = progress.getStatus();
                if (request.status() == Status.DONE && previousStatus != Status.DONE) {
                    progress.setCompletedAt(LocalDate.now());
                } else if (request.status() != Status.DONE) {
                    progress.setCompletedAt(null);
                }
                if (request.status() == Status.REVISE && previousStatus != Status.REVISE) {
                    progress.setReviewStage(0);
                    progress.setReviewDueAt(LocalDate.now().plusDays(REVIEW_INTERVALS[0]));
                } else if (request.status() != Status.REVISE) {
                    progress.setReviewStage(0);
                    progress.setReviewDueAt(null);
                }
                progress.setStatus(request.status());
            }
            if (request.notes() != null) {
                progress.setNotes(request.notes());
            }
            if (request.bookmarked() != null) {
                progress.setBookmarked(request.bookmarked());
            }
            progress = userProgressRepository.save(progress);
        } else {
            progress = userProgressRepository.findByUserIdAndProblemId(currentUser.getId(), problemId).orElse(null);
        }

        return ProblemResponse.from(problem, progress);
    }

    @Transactional
    public ProblemResponse review(User currentUser, Long problemId, ReviewOutcome outcome) {
        UserProgress progress = userProgressRepository.findByUserIdAndProblemId(currentUser.getId(), problemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No progress to review for problem " + problemId));

        if (progress.getStatus() != Status.REVISE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Problem " + problemId + " is not marked for review");
        }

        if (outcome == ReviewOutcome.REMEMBERED) {
            progress.setReviewStage(Math.min(progress.getReviewStage() + 1, REVIEW_INTERVALS.length - 1));
        } else {
            progress.setReviewStage(0);
        }
        progress.setReviewDueAt(LocalDate.now().plusDays(REVIEW_INTERVALS[progress.getReviewStage()]));
        progress = userProgressRepository.save(progress);

        return ProblemResponse.from(progress.getProblem(), progress);
    }
}
