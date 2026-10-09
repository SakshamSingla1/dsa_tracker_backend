package com.dsatracker.dto.admin;

import java.util.List;

public record RoleUpsertRequest(String name, String description, List<Long> permissionIds) {
}
