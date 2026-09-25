package com.dsatracker.dto;

/**
 * The judge's verdict on one test case. Input/expected/actual are only
 * populated for sample cases -- hidden cases only ever reveal pass/fail and
 * any stderr, same as a real judge.
 */
public record TestCaseResult(
        int index,
        boolean sample,
        String input,
        String expectedOutput,
        String actualOutput,
        boolean passed,
        boolean timedOut,
        String stderr
) {
}
