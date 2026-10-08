package com.dsatracker.dto;

public record RecommendationResponse(Long problemId, String title, String difficulty, String topicName, String reason) {
}
