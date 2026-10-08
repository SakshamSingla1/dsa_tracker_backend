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
        Long submissionId,
        /** XP earned from this submission -- null unless this was the first-ever ACCEPTED submit for the problem. */
        Long xpAwarded,
        /** Heuristic complexity estimate -- null on "Run" (sample-only) and on COMPILE_ERROR, present on every "Submit". */
        ComplexityEstimateResponse complexity
) {
}
