package com.dsatracker.service.admin;

import com.dsatracker.dto.admin.PermissionResponse;
import com.dsatracker.dto.admin.RoleResponse;
import com.dsatracker.dto.admin.RoleUpsertRequest;
import com.dsatracker.model.Permission;
import com.dsatracker.model.Role;
import com.dsatracker.repository.PermissionRepository;
import com.dsatracker.repository.RoleRepository;
import com.dsatracker.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdminRoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    public AdminRoleService(RoleRepository roleRepository, PermissionRepository permissionRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles() {
        return roleRepository.findAllByOrderByNameAsc().stream().map(RoleResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissions() {
        return permissionRepository.findAllByOrderByCodeAsc().stream().map(PermissionResponse::from).toList();
    }

    @Transactional
    public RoleResponse createRole(RoleUpsertRequest request) {
        validate(request);
        if (roleRepository.existsByName(request.name().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A role named '" + request.name() + "' already exists.");
        }
        Role role = new Role();
        applyRequest(role, request);
        return RoleResponse.from(roleRepository.save(role));
    }

    @Transactional
    public RoleResponse updateRole(Long id, RoleUpsertRequest request) {
        validate(request);
        Role role = findRole(id);
        roleRepository.findByName(request.name().trim())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "A role named '" + request.name() + "' already exists.");
                });
        applyRequest(role, request);
        return RoleResponse.from(roleRepository.save(role));
    }

    @Transactional
    public void deleteRole(Long id) {
        findRole(id);
        if (userRepository.existsByRoleId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unassign this role from every user before deleting it.");
        }
        roleRepository.deleteById(id);
    }

    private void validate(RoleUpsertRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required.");
        }
    }

    private void applyRequest(Role role, RoleUpsertRequest request) {
        role.setName(request.name().trim());
        role.setDescription(request.description());
        Set<Permission> permissions = new HashSet<>();
        if (request.permissionIds() != null) {
            for (Long permissionId : request.permissionIds()) {
                permissions.add(permissionRepository.findById(permissionId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No permission with id " + permissionId)));
            }
        }
        role.setPermissions(permissions);
    }

    private Role findRole(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No role with id " + id));
    }
}
