package com.dsatracker.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** A per-user rollup of their submission history, powering the "Insights" view. */
public record AnalyticsSummaryResponse(
        long totalSubmissions,
        long acceptedCount,
        double acceptanceRate,
        Map<String, Long> byVerdict,
        Map<String, Long> byLanguage,
        List<TopicBreakdown> byTopic,
        List<DailyCount> dailyActivity
) {
    public record TopicBreakdown(String topicName, long attempted, long accepted) {
    }

    public record DailyCount(LocalDate date, long count) {
    }
}
