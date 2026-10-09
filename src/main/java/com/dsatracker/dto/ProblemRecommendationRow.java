package com.dsatracker.dto;

import com.dsatracker.model.Difficulty;

/**
 * Lean projection of a problem for the recommendation engine's full-catalog scan
 * (AnalyticsService.getRecommendations()), which only ever needs id/title/difficulty/topic name
 * to pick unsolved problems in weak/unexplored topics. A full {@code Problem} entity fetch here
 * would eagerly load every problem's tags (a separate @ElementCollection table) across the
 * entire catalog for a result that never reads them.
 */
public record ProblemRecommendationRow(Long id, String title, Difficulty difficulty, String topicName) {
}
