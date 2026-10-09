package com.dsatracker.service.admin;

import com.dsatracker.dto.admin.AdminUserResponse;
import com.dsatracker.model.Role;
import com.dsatracker.model.User;
import com.dsatracker.repository.RoleRepository;
import com.dsatracker.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AdminUserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers() {
        return userRepository.findAll().stream().map(AdminUserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUser(Long id) {
        return AdminUserResponse.from(findUser(id));
    }

    @Transactional
    public AdminUserResponse assignRole(Long id, Long roleId) {
        User user = findUser(id);
        if (roleId == null) {
            user.setRole(null);
        } else {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No role with id " + roleId));
            user.setRole(role);
        }
        return AdminUserResponse.from(userRepository.save(user));
    }

    @Transactional
    public AdminUserResponse setEnabled(Long id, boolean enabled, User currentAdmin) {
        User user = findUser(id);
        if (!enabled && user.getId().equals(currentAdmin.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot disable your own account.");
        }
        user.setEnabled(enabled);
        return AdminUserResponse.from(userRepository.save(user));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No user with id " + id));
    }
}
