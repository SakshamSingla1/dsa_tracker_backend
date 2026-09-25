package com.dsatracker.service;

import com.dsatracker.dto.AnalyticsSummaryResponse;
import com.dsatracker.model.Submission;
import com.dsatracker.model.Verdict;
import com.dsatracker.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    private final SubmissionRepository submissionRepository;

    public AnalyticsService(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getSummary(Long userId, int days) {
        List<Submission> submissions = submissionRepository.findAllByUserIdWithProblemAndTopic(userId);

        long total = submissions.size();
        long accepted = submissions.stream().filter(s -> s.getVerdict() == Verdict.ACCEPTED).count();
        double acceptanceRate = total == 0 ? 0.0 : (double) accepted / total;

        Map<String, Long> byVerdict = submissions.stream()
                .collect(Collectors.groupingBy(s -> s.getVerdict().name(), LinkedHashMap::new, Collectors.counting()));

        Map<String, Long> byLanguage = submissions.stream()
                .collect(Collectors.groupingBy(s -> s.getLanguage().name(), LinkedHashMap::new, Collectors.counting()));

        Map<String, long[]> topicAgg = new LinkedHashMap<>();
        for (Submission s : submissions) {
            String topicName = s.getProblem().getTopic().getName();
            long[] counts = topicAgg.computeIfAbsent(topicName, k -> new long[2]);
            counts[0]++;
            if (s.getVerdict() == Verdict.ACCEPTED) counts[1]++;
        }
        List<AnalyticsSummaryResponse.TopicBreakdown> byTopic = topicAgg.entrySet().stream()
                .map(e -> new AnalyticsSummaryResponse.TopicBreakdown(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .sorted(Comparator.comparingLong(AnalyticsSummaryResponse.TopicBreakdown::attempted).reversed())
                .toList();

        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        ZoneId zone = ZoneId.systemDefault();
        Map<LocalDate, Long> dailyAgg = submissions.stream()
                .filter(s -> !s.getSubmittedAt().isBefore(cutoff))
                .collect(Collectors.groupingBy(s -> s.getSubmittedAt().atZone(zone).toLocalDate(), Collectors.counting()));
        List<AnalyticsSummaryResponse.DailyCount> dailyActivity = dailyAgg.entrySet().stream()
                .map(e -> new AnalyticsSummaryResponse.DailyCount(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(AnalyticsSummaryResponse.DailyCount::date))
                .toList();

        return new AnalyticsSummaryResponse(total, accepted, acceptanceRate, byVerdict, byLanguage, byTopic, dailyActivity);
    }
}
