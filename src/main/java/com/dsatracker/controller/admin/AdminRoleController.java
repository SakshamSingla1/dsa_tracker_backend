package com.dsatracker.controller.admin;

import com.dsatracker.dto.admin.PermissionResponse;
import com.dsatracker.dto.admin.RoleResponse;
import com.dsatracker.dto.admin.RoleUpsertRequest;
import com.dsatracker.service.admin.AdminRoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_MANAGE')")
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    public AdminRoleController(AdminRoleService adminRoleService) {
        this.adminRoleService = adminRoleService;
    }

    @GetMapping("/roles")
    public List<RoleResponse> listRoles() {
        return adminRoleService.listRoles();
    }

    @GetMapping("/permissions")
    public List<PermissionResponse> listPermissions() {
        return adminRoleService.listPermissions();
    }

    @PostMapping("/roles")
    public RoleResponse create(@RequestBody RoleUpsertRequest request) {
        return adminRoleService.createRole(request);
    }

    @PutMapping("/roles/{id}")
    public RoleResponse update(@PathVariable Long id, @RequestBody RoleUpsertRequest request) {
        return adminRoleService.updateRole(id, request);
    }

    @DeleteMapping("/roles/{id}")
    public void delete(@PathVariable Long id) {
        adminRoleService.deleteRole(id);
    }
}
