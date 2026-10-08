package com.dsatracker.dto;

/** Heuristic time-complexity estimate for a submission, from {@code ComplexityAnalyzerService}. */
public record ComplexityEstimateResponse(int maxLoopDepth, boolean likelyRecursive, String estimate) {
}
