package com.dsatracker.service;

import com.dsatracker.dto.AnalyticsSummaryResponse;
import com.dsatracker.dto.ProblemRecommendationRow;
import com.dsatracker.dto.RecommendationResponse;
import com.dsatracker.dto.SubmissionAnalyticsRow;
import com.dsatracker.model.Difficulty;
import com.dsatracker.model.Status;
import com.dsatracker.model.UserProgress;
import com.dsatracker.model.Verdict;
import com.dsatracker.repository.ProblemRepository;
import com.dsatracker.repository.SubmissionRepository;
import com.dsatracker.repository.UserProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Aggregates a user's submission history (verdict mix, language mix, per-topic
 * attempt/accept counts, daily activity) for the Insights view. Everything is
 * computed in memory off one eagerly-joined query -- submission volumes for a
 * personal practice tool are small enough that this is simpler and more
 * portable than DB-specific date-grouping SQL.
 */
@Service
public class AnalyticsService {

    private static final int MAX_RECOMMENDATIONS = 6;
    private static final int MAX_PER_TOPIC = 2;
    /** A topic needs at least this many attempts before its acceptance rate is trusted as "weak" --
     *  otherwise one unlucky early submission would misfire a recommendation. */
    private static final int MIN_ATTEMPTS_FOR_SIGNAL = 2;
    private static final double WEAK_ACCEPTANCE_THRESHOLD = 0.6;

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final UserProgressRepository userProgressRepository;

    public AnalyticsService(
            SubmissionRepository submissionRepository,
            ProblemRepository problemRepository,
            UserProgressRepository userProgressRepository
    ) {
        this.submissionRepository = submissionRepository;
        this.problemRepository = problemRepository;
        this.userProgressRepository = userProgressRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getSummary(Long userId, int days) {
        List<SubmissionAnalyticsRow> submissions = submissionRepository.findAnalyticsRowsByUserId(userId);

        long total = submissions.size();
        long accepted = submissions.stream().filter(s -> s.verdict() == Verdict.ACCEPTED).count();
        double acceptanceRate = total == 0 ? 0.0 : (double) accepted / total;

        Map<String, Long> byVerdict = submissions.stream()
                .collect(Collectors.groupingBy(s -> s.verdict().name(), LinkedHashMap::new, Collectors.counting()));

        Map<String, Long> byLanguage = submissions.stream()
                .collect(Collectors.groupingBy(s -> s.language().name(), LinkedHashMap::new, Collectors.counting()));

        Map<String, long[]> topicAgg = new LinkedHashMap<>();
        for (SubmissionAnalyticsRow s : submissions) {
            long[] counts = topicAgg.computeIfAbsent(s.topicName(), k -> new long[2]);
            counts[0]++;
            if (s.verdict() == Verdict.ACCEPTED) counts[1]++;
        }
        List<AnalyticsSummaryResponse.TopicBreakdown> byTopic = topicAgg.entrySet().stream()
                .map(e -> new AnalyticsSummaryResponse.TopicBreakdown(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .sorted(Comparator.comparingLong(AnalyticsSummaryResponse.TopicBreakdown::attempted).reversed())
                .toList();

        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        ZoneId zone = ZoneId.systemDefault();
        Map<LocalDate, Long> dailyAgg = submissions.stream()
                .filter(s -> !s.submittedAt().isBefore(cutoff))
                .collect(Collectors.groupingBy(s -> s.submittedAt().atZone(zone).toLocalDate(), Collectors.counting()));
        List<AnalyticsSummaryResponse.DailyCount> dailyActivity = dailyAgg.entrySet().stream()
                .map(e -> new AnalyticsSummaryResponse.DailyCount(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(AnalyticsSummaryResponse.DailyCount::date))
                .toList();

        return new AnalyticsSummaryResponse(total, accepted, acceptanceRate, byVerdict, byLanguage, byTopic, dailyActivity);
    }

    /**
     * Rule-based "what to solve next": topics the user is weakest on (lowest acceptance rate,
     * with enough attempts to trust the signal) first, falling back to topics they haven't
     * touched at all when there isn't enough submission history yet to find a weak spot.
     * Within a topic, easier unsolved problems are preferred -- the point is to rebuild
     * confidence on the weak topic, not to immediately hand them its hardest problem.
     */
    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendations(Long userId) {
        List<SubmissionAnalyticsRow> submissions = submissionRepository.findAnalyticsRowsByUserId(userId);

        Map<String, long[]> topicAgg = new LinkedHashMap<>(); // name -> [attempted, accepted]
        for (SubmissionAnalyticsRow s : submissions) {
            long[] counts = topicAgg.computeIfAbsent(s.topicName(), k -> new long[2]);
            counts[0]++;
            if (s.verdict() == Verdict.ACCEPTED) counts[1]++;
        }

        List<String> weakTopics = topicAgg.entrySet().stream()
                .filter(e -> e.getValue()[0] >= MIN_ATTEMPTS_FOR_SIGNAL)
                .filter(e -> (double) e.getValue()[1] / e.getValue()[0] < WEAK_ACCEPTANCE_THRESHOLD)
                .sorted(Comparator.comparingDouble(e -> (double) e.getValue()[1] / e.getValue()[0]))
                .map(Map.Entry::getKey)
                .toList();

        Map<Long, UserProgress> progressByProblemId = userProgressRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(up -> up.getProblem().getId(), p -> p));
        List<ProblemRecommendationRow> allProblems = problemRepository.findAllForRecommendations();

        List<RecommendationResponse> recommendations = new ArrayList<>();
        if (!weakTopics.isEmpty()) {
            for (String topicName : weakTopics) {
                if (recommendations.size() >= MAX_RECOMMENDATIONS) break;
                long[] counts = topicAgg.get(topicName);
                String reason = "You're at %d%% acceptance on %s -- a bit more practice here should help"
                        .formatted(Math.round(100.0 * counts[1] / counts[0]), topicName);
                addUnsolvedFromTopic(recommendations, allProblems, progressByProblemId, topicName, reason);
            }
        }

        if (recommendations.isEmpty()) {
            Set<String> attemptedTopics = topicAgg.keySet();
            Set<String> unexploredTopics = allProblems.stream()
                    .map(ProblemRecommendationRow::topicName)
                    .filter(name -> !attemptedTopics.contains(name))
                    .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
            for (String topicName : unexploredTopics) {
                if (recommendations.size() >= MAX_RECOMMENDATIONS) break;
                addUnsolvedFromTopic(recommendations, allProblems, progressByProblemId, topicName,
                        "You haven't tried " + topicName + " yet -- a good place to start");
            }
        }

        return recommendations;
    }

    private void addUnsolvedFromTopic(
            List<RecommendationResponse> recommendations, List<ProblemRecommendationRow> allProblems,
            Map<Long, UserProgress> progressByProblemId, String topicName, String reason
    ) {
        allProblems.stream()
                .filter(p -> p.topicName().equals(topicName))
                .filter(p -> {
                    UserProgress progress = progressByProblemId.get(p.id());
                    return progress == null || progress.getStatus() != Status.DONE;
                })
                .sorted(Comparator.comparing(p -> difficultyRank(p.difficulty())))
                .limit(MAX_PER_TOPIC)
                .forEach(p -> recommendations.add(new RecommendationResponse(
                        p.id(), p.title(), p.difficulty().name(), topicName, reason
                )));
    }

    private int difficultyRank(Difficulty difficulty) {
        if (difficulty == null) return 1;
        return switch (difficulty) {
            case EASY -> 0;
            case MEDIUM -> 1;
            case HARD -> 2;
        };
    }
}
