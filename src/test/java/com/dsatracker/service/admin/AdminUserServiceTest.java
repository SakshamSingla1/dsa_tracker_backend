package com.dsatracker.service.admin;

import com.dsatracker.model.Role;
import com.dsatracker.model.User;
import com.dsatracker.repository.RoleRepository;
import com.dsatracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;

    private AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserService(userRepository, roleRepository);
    }

    private User userWithId(long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@example.com");
        user.setDisplayName("User " + id);
        return user;
    }

    @Test
    void setEnabled_rejectsDisablingYourOwnAccount() {
        User admin = userWithId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> adminUserService.setEnabled(1L, false, admin))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("cannot disable your own account");
    }

    @Test
    void setEnabled_allowsDisablingSomeoneElse() {
        User admin = userWithId(1L);
        User target = userWithId(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = adminUserService.setEnabled(2L, false, admin);

        assertThat(response.enabled()).isFalse();
    }

    @Test
    void assignRole_withNullRoleId_unassignsRole() {
        User target = userWithId(2L);
        Role role = new Role();
        role.setId(5L);
        role.setName("Content Manager");
        target.setRole(role);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = adminUserService.assignRole(2L, null);

        assertThat(response.roleId()).isNull();
    }

    @Test
    void assignRole_rejectsUnknownRoleId() {
        User target = userWithId(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.assignRole(2L, 99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No role with id 99");
    }
}
