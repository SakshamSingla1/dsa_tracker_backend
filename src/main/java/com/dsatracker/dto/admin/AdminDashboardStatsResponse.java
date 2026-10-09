package com.dsatracker.dto.admin;

public record AdminDashboardStatsResponse(
        long totalUsers, long totalSheets, long totalTopics, long totalProblems,
        long submissionsLast7Days, long activeAdmins
) {
}
