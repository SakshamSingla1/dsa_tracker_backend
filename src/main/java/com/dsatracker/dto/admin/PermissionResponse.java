package com.dsatracker.dto.admin;

import com.dsatracker.model.Permission;

public record PermissionResponse(Long id, String code, String description) {
    public static PermissionResponse from(Permission p) {
        return new PermissionResponse(p.getId(), p.getCode(), p.getDescription());
    }
}
