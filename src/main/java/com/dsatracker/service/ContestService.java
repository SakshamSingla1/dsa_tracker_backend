package com.dsatracker.service;

import com.dsatracker.dto.ContestProblemResponse;
import com.dsatracker.dto.ContestSessionResponse;
import com.dsatracker.dto.StartContestRequest;
import com.dsatracker.model.ContestProblem;
import com.dsatracker.model.ContestSession;
import com.dsatracker.model.ContestStatus;
import com.dsatracker.model.Difficulty;
import com.dsatracker.model.Problem;
import com.dsatracker.model.Status;
import com.dsatracker.model.Topic;
import com.dsatracker.model.User;
import com.dsatracker.repository.ContestProblemRepository;
import com.dsatracker.repository.ContestSessionRepository;
import com.dsatracker.repository.TopicRepository;
import com.dsatracker.repository.UserProgressRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Self-service timed practice sessions: no admin-curated contest catalog -- a session is a
 * fresh problem set drawn on demand for its owner, scored purely by "solved within the window."
 */
@Service
public class ContestService {

    private static final int MIN_PROBLEMS = 1;
    private static final int MAX_PROBLEMS = 20;
    private static final int MIN_DURATION_MINUTES = 5;
    private static final int MAX_DURATION_MINUTES = 180;

    private final ContestSessionRepository contestSessionRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final TopicRepository topicRepository;
    private final UserProgressRepository userProgressRepository;

    public ContestService(
            ContestSessionRepository contestSessionRepository,
            ContestProblemRepository contestProblemRepository,
            TopicRepository topicRepository,
            UserProgressRepository userProgressRepository
    ) {
        this.contestSessionRepository = contestSessionRepository;
        this.contestProblemRepository = contestProblemRepository;
        this.topicRepository = topicRepository;
        this.userProgressRepository = userProgressRepository;
    }

    @Transactional
    public ContestSessionResponse start(User user, StartContestRequest request) {
        int problemCount = clamp(request.problemCount(), MIN_PROBLEMS, MAX_PROBLEMS);
        int durationMinutes = clamp(request.durationMinutes(), MIN_DURATION_MINUTES, MAX_DURATION_MINUTES);
        Difficulty difficulty = parseDifficulty(request.difficulty());
        String sheetSlug = normalizeSheetSlug(request.sheetSlug());

        List<Topic> topics = sheetSlug != null
                ? topicRepository.findAllBySheetSlugOrderByOrderIndexAsc(sheetSlug)
                : topicRepository.findAllByOrderByOrderIndexAsc();

        List<Problem> candidates = topics.stream()
                .flatMap(t -> t.getProblems().stream())
                .filter(p -> difficulty == null || p.getDifficulty() == difficulty)
                .toList();

        if (candidates.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No problems match that sheet/difficulty.");
        }

        Set<Long> doneProblemIds = userProgressRepository.findAllByUserIdAndStatus(user.getId(), Status.DONE).stream()
                .map(up -> up.getProblem().getId())
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));

        List<Problem> notDone = new ArrayList<>(candidates.stream().filter(p -> !doneProblemIds.contains(p.getId())).toList());
        List<Problem> done = new ArrayList<>(candidates.stream().filter(p -> doneProblemIds.contains(p.getId())).toList());
        Collections.shuffle(notDone);
        Collections.shuffle(done);

        List<Problem> picked = new ArrayList<>(notDone.subList(0, Math.min(problemCount, notDone.size())));
        if (picked.size() < problemCount) {
            int remaining = problemCount - picked.size();
            picked.addAll(done.subList(0, Math.min(remaining, done.size())));
        }

        ContestSession session = new ContestSession();
        session.setUser(user);
        session.setDurationMinutes(durationMinutes);
        session.setDifficulty(difficulty);
        session.setSheetSlug(sheetSlug);
        session = contestSessionRepository.save(session);

        List<ContestProblem> contestProblems = new ArrayList<>();
        for (int i = 0; i < picked.size(); i++) {
            ContestProblem cp = new ContestProblem();
            cp.setContestSession(session);
            cp.setProblem(picked.get(i));
            cp.setOrderIndex(i);
            contestProblems.add(cp);
        }
        contestProblems = contestProblemRepository.saveAll(contestProblems);

        return toResponse(session, contestProblems);
    }

    @Transactional
    public ContestSessionResponse get(User user, Long contestId) {
        ContestSession session = findOwned(user, contestId);
        autoExpireIfDue(session);
        List<ContestProblem> problems = contestProblemRepository.findAllByContestSessionIdOrderByOrderIndexAsc(session.getId());
        return toResponse(session, problems);
    }

    @Transactional
    public ContestSessionResponse finish(User user, Long contestId) {
        ContestSession session = findOwned(user, contestId);
        if (session.getStatus() == ContestStatus.IN_PROGRESS) {
            session.setStatus(ContestStatus.FINISHED);
            session.setFinishedAt(Instant.now());
            session = contestSessionRepository.save(session);
        }
        List<ContestProblem> problems = contestProblemRepository.findAllByContestSessionIdOrderByOrderIndexAsc(session.getId());
        return toResponse(session, problems);
    }

    @Transactional(readOnly = true)
    public List<ContestSessionResponse> history(User user) {
        return contestSessionRepository.findAllByUserIdOrderByStartedAtDesc(user.getId()).stream()
                .map(session -> {
                    List<ContestProblem> problems = contestProblemRepository.findAllByContestSessionIdOrderByOrderIndexAsc(session.getId());
                    return toResponse(session, problems);
                })
                .toList();
    }

    /** Called by JudgeService on an ACCEPTED submit. A no-op unless the user has an active
     *  session (still within its time window) containing this problem, unsolved. */
    @Transactional
    public void markSolvedIfActive(User user, Long problemId) {
        List<ContestSession> active = contestSessionRepository.findAllByUserIdAndStatus(user.getId(), ContestStatus.IN_PROGRESS);
        for (ContestSession session : active) {
            if (Instant.now().isAfter(session.getEndsAt())) continue;
            contestProblemRepository.findByContestSessionIdAndProblemId(session.getId(), problemId)
                    .filter(cp -> cp.getSolvedAt() == null)
                    .ifPresent(cp -> {
                        cp.setSolvedAt(Instant.now());
                        contestProblemRepository.save(cp);
                    });
        }
    }

    private ContestSession findOwned(User user, Long contestId) {
        return contestSessionRepository.findByIdAndUserId(contestId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No contest with id " + contestId));
    }

    private void autoExpireIfDue(ContestSession session) {
        if (session.getStatus() == ContestStatus.IN_PROGRESS && Instant.now().isAfter(session.getEndsAt())) {
            session.setStatus(ContestStatus.FINISHED);
            session.setFinishedAt(Instant.now());
            contestSessionRepository.save(session);
        }
    }

    private ContestSessionResponse toResponse(ContestSession session, List<ContestProblem> contestProblems) {
        List<ContestProblemResponse> problemResponses = contestProblems.stream()
                .sorted(Comparator.comparingInt(ContestProblem::getOrderIndex))
                .map(cp -> new ContestProblemResponse(
                        cp.getProblem().getId(),
                        cp.getProblem().getTitle(),
                        cp.getProblem().getDifficulty(),
                        cp.getOrderIndex(),
                        cp.getSolvedAt() != null,
                        cp.getSolvedAt()
                ))
                .toList();
        int solvedCount = (int) problemResponses.stream().filter(ContestProblemResponse::solved).count();

        return new ContestSessionResponse(
                session.getId(),
                session.getStartedAt(),
                session.getEndsAt(),
                session.getDurationMinutes(),
                session.getStatus(),
                session.getFinishedAt(),
                solvedCount,
                problemResponses.size(),
                problemResponses
        );
    }

    private Difficulty parseDifficulty(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Difficulty.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown difficulty: " + raw);
        }
    }

    private String normalizeSheetSlug(String raw) {
        if (raw == null || raw.isBlank() || raw.equalsIgnoreCase("ALL")) return null;
        return raw;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
