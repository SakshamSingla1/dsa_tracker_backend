package com.dsatracker.service.admin;

import com.dsatracker.dto.admin.RoleUpsertRequest;
import com.dsatracker.model.Permission;
import com.dsatracker.model.Role;
import com.dsatracker.repository.PermissionRepository;
import com.dsatracker.repository.RoleRepository;
import com.dsatracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRoleServiceTest {

    @Mock RoleRepository roleRepository;
    @Mock PermissionRepository permissionRepository;
    @Mock UserRepository userRepository;

    private AdminRoleService adminRoleService;

    @BeforeEach
    void setUp() {
        adminRoleService = new AdminRoleService(roleRepository, permissionRepository, userRepository);
    }

    private Permission permission(long id, String code) {
        Permission p = new Permission();
        p.setId(id);
        p.setCode(code);
        return p;
    }

    @Test
    void createRole_rejectsDuplicateName() {
        when(roleRepository.existsByName("Content Manager")).thenReturn(true);

        assertThatThrownBy(() -> adminRoleService.createRole(new RoleUpsertRequest("Content Manager", "desc", List.of())))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createRole_attachesRequestedPermissions() {
        when(roleRepository.existsByName("Reviewer")).thenReturn(false);
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission(1L, "PROBLEM_MANAGE")));
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = adminRoleService.createRole(new RoleUpsertRequest("Reviewer", "desc", List.of(1L)));

        assertThat(response.permissionCodes()).containsExactly("PROBLEM_MANAGE");
    }

    @Test
    void createRole_rejectsUnknownPermissionId() {
        when(roleRepository.existsByName("Reviewer")).thenReturn(false);
        when(permissionRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminRoleService.createRole(new RoleUpsertRequest("Reviewer", "desc", List.of(404L))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No permission with id 404");
    }

    @Test
    void deleteRole_rejectsWhenStillAssignedToAUser() {
        Role role = new Role();
        role.setId(3L);
        when(roleRepository.findById(3L)).thenReturn(Optional.of(role));
        when(userRepository.existsByRoleId(3L)).thenReturn(true);

        assertThatThrownBy(() -> adminRoleService.deleteRole(3L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unassign this role");
    }

    @Test
    void deleteRole_succeedsWhenUnassigned() {
        Role role = new Role();
        role.setId(3L);
        when(roleRepository.findById(3L)).thenReturn(Optional.of(role));
        when(userRepository.existsByRoleId(3L)).thenReturn(false);

        adminRoleService.deleteRole(3L);

        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        verify(roleRepository).deleteById(idCaptor.capture());
        assertThat(idCaptor.getValue()).isEqualTo(3L);
    }
}
