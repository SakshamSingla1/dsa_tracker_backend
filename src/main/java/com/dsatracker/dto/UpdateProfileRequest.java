package com.dsatracker.dto;

/**
 * Partial update payload for a user's profile. A null field is left unchanged
 * on the entity, matching {@link ProblemUpdateRequest}'s convention.
 */
public record UpdateProfileRequest(String displayName, String bio) {
}
