package com.dsatracker.controller.admin;

import com.dsatracker.dto.admin.AdminUserResponse;
import com.dsatracker.dto.admin.AssignRoleRequest;
import com.dsatracker.dto.admin.SetEnabledRequest;
import com.dsatracker.model.User;
import com.dsatracker.service.admin.AdminUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasAuthority('USER_MANAGE')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public List<AdminUserResponse> list() {
        return adminUserService.listUsers();
    }

    @GetMapping("/{id}")
    public AdminUserResponse get(@PathVariable Long id) {
        return adminUserService.getUser(id);
    }

    @PatchMapping("/{id}/role")
    public AdminUserResponse assignRole(@PathVariable Long id, @RequestBody AssignRoleRequest request) {
        return adminUserService.assignRole(id, request.roleId());
    }

    @PatchMapping("/{id}/enabled")
    public AdminUserResponse setEnabled(
            @AuthenticationPrincipal User currentAdmin,
            @PathVariable Long id,
            @RequestBody SetEnabledRequest request
    ) {
        return adminUserService.setEnabled(id, request.enabled(), currentAdmin);
    }
}
