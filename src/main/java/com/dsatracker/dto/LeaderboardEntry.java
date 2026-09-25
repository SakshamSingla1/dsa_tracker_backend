package com.dsatracker.dto;

public record LeaderboardEntry(String displayName, long solvedCount, boolean isYou) {
}
