package com.dsatracker.dto;

import com.dsatracker.model.ContestStatus;

import java.time.Instant;
import java.util.List;

public record ContestSessionResponse(
        Long id,
        Instant startedAt,
        Instant endsAt,
        int durationMinutes,
        ContestStatus status,
        Instant finishedAt,
        int solvedCount,
        int totalCount,
        List<ContestProblemResponse> problems
) {
}
