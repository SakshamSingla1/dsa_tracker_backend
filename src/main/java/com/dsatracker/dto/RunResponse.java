package com.dsatracker.dto;

public record RunResponse(
        boolean compiled,
        boolean timedOut,
        String stdout,
        String stderr,
        Integer exitCode,
        long durationMs
) {
}
