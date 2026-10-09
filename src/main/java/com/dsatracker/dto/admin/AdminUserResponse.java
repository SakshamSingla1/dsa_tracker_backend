package com.dsatracker.dto.admin;

import com.dsatracker.model.User;

import java.time.Instant;

public record AdminUserResponse(
        Long id, String email, String displayName, boolean enabled,
        Long roleId, String roleName, Instant createdAt
) {
    public static AdminUserResponse from(User u) {
        return new AdminUserResponse(
                u.getId(), u.getEmail(), u.getDisplayName(), u.isEnabled(),
                u.getRole() != null ? u.getRole().getId() : null,
                u.getRole() != null ? u.getRole().getName() : null,
                u.getCreatedAt()
        );
    }
}
