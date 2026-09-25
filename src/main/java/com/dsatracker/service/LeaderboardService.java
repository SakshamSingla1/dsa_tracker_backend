package com.dsatracker.service;

import com.dsatracker.dto.LeaderboardEntry;
import com.dsatracker.model.Status;
import com.dsatracker.model.User;
import com.dsatracker.model.UserProgress;
import com.dsatracker.model.Verdict;
import com.dsatracker.repository.SubmissionRepository;
import com.dsatracker.repository.SubmissionRepository.UserSolvedCount;
import com.dsatracker.repository.UserProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Ranks users by how many problems they've solved, across every sheet by default.
 * Deliberately minimal -- just a name and a count, nothing else about other
 * users is exposed. Supports an optional time scope (this week / this month,
 * driven by submission history) and an optional sheet filter.
 */
@Service
public class LeaderboardService {

    private static final int MAX_ENTRIES = 50;

    private final UserProgressRepository userProgressRepository;
    private final SubmissionRepository submissionRepository;

    public LeaderboardService(UserProgressRepository userProgressRepository, SubmissionRepository submissionRepository) {
        this.userProgressRepository = userProgressRepository;
        this.submissionRepository = submissionRepository;
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntry> getLeaderboard(Long currentUserId, String scope, String sheetSlug) {
        String normalizedSheet = normalizeSheet(sheetSlug);
        Map<User, Long> byUser = "WEEK".equalsIgnoreCase(scope) || "MONTH".equalsIgnoreCase(scope)
                ? countRecentlyAccepted(scope, normalizedSheet)
                : countAllTimeDone(normalizedSheet);

        return byUser.entrySet().stream()
                .sorted(Map.Entry.<User, Long>comparingByValue().reversed())
                .limit(MAX_ENTRIES)
                .map(e -> new LeaderboardEntry(e.getKey().getDisplayName(), e.getValue(), e.getKey().getId().equals(currentUserId)))
                .toList();
    }

    private Map<User, Long> countAllTimeDone(String sheetSlug) {
        List<UserProgress> progress = sheetSlug == null
                ? userProgressRepository.findAllByStatus(Status.DONE)
                : userProgressRepository.findAllByStatusAndProblem_Topic_Sheet_Slug(Status.DONE, sheetSlug);
        return progress.stream().collect(Collectors.groupingBy(UserProgress::getUser, Collectors.counting()));
    }

    private Map<User, Long> countRecentlyAccepted(String scope, String sheetSlug) {
        int days = "WEEK".equalsIgnoreCase(scope) ? 7 : 30;
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        return submissionRepository.countDistinctAcceptedProblemsSince(Verdict.ACCEPTED, cutoff, sheetSlug).stream()
                .collect(Collectors.toMap(UserSolvedCount::getUser, UserSolvedCount::getCnt));
    }

    private String normalizeSheet(String sheetSlug) {
        if (sheetSlug == null || sheetSlug.isBlank() || sheetSlug.equalsIgnoreCase("all")) return null;
        return sheetSlug;
    }
}
