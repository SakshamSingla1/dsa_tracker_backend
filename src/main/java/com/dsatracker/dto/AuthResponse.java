package com.dsatracker.dto;

import java.util.List;

/** {@code permissions} is empty for ordinary students, populated for users with an admin
 *  {@link com.dsatracker.model.Role} -- lets the admin portal render its permission-gated
 *  sidebar right after login without a second round trip. */
public record AuthResponse(String token, Long userId, String email, String displayName, List<String> permissions) {
}
