package com.dsatracker.dto;

import com.dsatracker.model.Difficulty;
import com.dsatracker.model.InterviewStatus;

import java.time.Instant;
import java.util.List;

public record InterviewSessionResponse(
        Long id,
        Long problemId,
        String problemTitle,
        Difficulty problemDifficulty,
        Instant startedAt,
        Instant endedAt,
        InterviewStatus status,
        String feedback,
        boolean aiAvailable,
        List<InterviewMessageResponse> messages
) {
}
