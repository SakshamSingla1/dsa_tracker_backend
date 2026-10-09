package com.dsatracker.dto.admin;

/** {@code roleId} null un-assigns the role, reverting the user to an ordinary student. */
public record AssignRoleRequest(Long roleId) {
}
