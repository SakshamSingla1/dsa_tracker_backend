package com.dsatracker.dto;

import java.time.Instant;

public record ProfileResponse(Long id, String email, String displayName, String bio, Instant createdAt) {
}
