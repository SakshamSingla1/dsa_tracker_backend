package com.dsatracker.dto;

import com.dsatracker.model.Language;
import com.dsatracker.model.Verdict;

import java.time.Instant;

/**
 * Lean projection of a submission for verdict/language/topic/day aggregation (AnalyticsService).
 * Deliberately excludes {@code Submission.code} (a @Lob up to 50,000 chars) -- analytics never
 * reads it, but loading it for every submission in a user's history (via a full-entity fetch)
 * was the actual cause behind /api/analytics/summary's multi-second response time: not query
 * count, but transferring potentially megabytes of code text nobody asked for.
 */
public record SubmissionAnalyticsRow(Verdict verdict, Language language, Instant submittedAt, String topicName) {
}
