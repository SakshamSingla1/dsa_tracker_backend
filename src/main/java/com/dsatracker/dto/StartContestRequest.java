package com.dsatracker.dto;

/** difficulty: "EASY"/"MEDIUM"/"HARD" or null/blank for any. sheetSlug: a sheet's slug, or null/"ALL" for any sheet. */
public record StartContestRequest(int problemCount, String difficulty, String sheetSlug, int durationMinutes) {
}
