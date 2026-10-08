package com.dsatracker.dto;

import java.time.Instant;

public record ProfileResponse(
        Long id,
        String email,
        String displayName,
        String bio,
        long xp,
        int level,
        long xpIntoLevel,
        long xpForNextLevel,
        Instant createdAt
) {
}
