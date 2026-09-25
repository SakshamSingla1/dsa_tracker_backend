package com.dsatracker.dto;

import com.dsatracker.model.Difficulty;

import java.time.Instant;

public record ContestProblemResponse(
        Long problemId,
        String title,
        Difficulty difficulty,
        int orderIndex,
        boolean solved,
        Instant solvedAt
) {
}
