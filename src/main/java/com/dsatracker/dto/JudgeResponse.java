package com.dsatracker.dto;

import com.dsatracker.model.Verdict;

import java.util.List;

public record JudgeResponse(
        Verdict verdict,
        String compileError,
        int passedCount,
        int totalCount,
        long durationMs,
        List<TestCaseResult> results,
        Long submissionId
) {
}
