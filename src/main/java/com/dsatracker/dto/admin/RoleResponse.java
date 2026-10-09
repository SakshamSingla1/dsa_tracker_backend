package com.dsatracker.dto.admin;

import com.dsatracker.model.Permission;
import com.dsatracker.model.Role;

import java.util.Comparator;

public record RoleResponse(Long id, String name, String description, java.util.List<String> permissionCodes) {
    public static RoleResponse from(Role r) {
        return new RoleResponse(
                r.getId(), r.getName(), r.getDescription(),
                r.getPermissions().stream().map(Permission::getCode).sorted(Comparator.naturalOrder()).toList()
        );
    }
}
