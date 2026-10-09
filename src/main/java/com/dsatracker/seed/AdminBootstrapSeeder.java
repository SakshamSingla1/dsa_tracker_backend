package com.dsatracker.seed;

import com.dsatracker.model.Permission;
import com.dsatracker.model.Role;
import com.dsatracker.model.User;
import com.dsatracker.repository.PermissionRepository;
import com.dsatracker.repository.RoleRepository;
import com.dsatracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Seeds the admin-portal role/permission catalog on every boot (idempotent, unconditional --
 * the catalog itself isn't sensitive), then optionally assigns the "Super Admin" role to one
 * account named by {@code app.admin.bootstrap-email} -- the only way to create the first admin,
 * since there's no self-service "become admin" flow.
 *
 * <p>Deliberately safe to leave configured permanently as a "break glass" mechanism:
 * <ul>
 *   <li>unset/blank email -&gt; no-op</li>
 *   <li>named account doesn't exist yet -&gt; no-op (never creates a user)</li>
 *   <li>named account already has a role -&gt; no-op (never clobbers an existing assignment,
 *       so demoting an admin via the admin UI survives a redeploy/restart)</li>
 * </ul>
 */
@Component
public class AdminBootstrapSeeder implements CommandLineRunner {

    private record PermissionDef(String code, String description) {
    }

    private static final List<PermissionDef> PERMISSIONS = List.of(
            new PermissionDef("SHEET_MANAGE", "Create, edit, and delete problem sheets"),
            new PermissionDef("TOPIC_MANAGE", "Create, edit, delete, and reorder topics"),
            new PermissionDef("PROBLEM_MANAGE", "Create, edit, and delete problems"),
            new PermissionDef("USER_MANAGE", "View users, assign roles, enable/disable accounts"),
            new PermissionDef("ROLE_MANAGE", "Create and edit roles, assign permissions to roles"),
            new PermissionDef("CONFIG_MANAGE", "Read and write the generic meta-configuration store"),
            new PermissionDef("DASHBOARD_VIEW", "View the admin stats dashboard")
    );

    private static final String SUPER_ADMIN = "Super Admin";
    private static final String CONTENT_MANAGER = "Content Manager";

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    @Value("${app.admin.bootstrap-email:}")
    private String bootstrapEmail;

    public AdminBootstrapSeeder(RoleRepository roleRepository, PermissionRepository permissionRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Set<Permission> allPermissions = ensurePermissions();
        Role superAdmin = ensureRole(SUPER_ADMIN, "Full access to every admin screen", allPermissions);
        ensureRole(CONTENT_MANAGER, "Manages sheets, topics, and problems only", contentPermissions(allPermissions));

        assignBootstrapAdminIfConfigured(superAdmin);
    }

    private Set<Permission> ensurePermissions() {
        Set<Permission> result = new HashSet<>();
        for (PermissionDef def : PERMISSIONS) {
            Permission permission = permissionRepository.findByCode(def.code()).orElseGet(() -> {
                Permission p = new Permission();
                p.setCode(def.code());
                p.setDescription(def.description());
                return permissionRepository.save(p);
            });
            result.add(permission);
        }
        return result;
    }

    private Set<Permission> contentPermissions(Set<Permission> all) {
        Set<String> codes = Set.of("SHEET_MANAGE", "TOPIC_MANAGE", "PROBLEM_MANAGE");
        Set<Permission> subset = new HashSet<>();
        for (Permission p : all) {
            if (codes.contains(p.getCode())) {
                subset.add(p);
            }
        }
        return subset;
    }

    private Role ensureRole(String name, String description, Set<Permission> permissions) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role role = new Role();
            role.setName(name);
            role.setDescription(description);
            role.setPermissions(permissions);
            return roleRepository.save(role);
        });
    }

    private void assignBootstrapAdminIfConfigured(Role superAdmin) {
        if (bootstrapEmail == null || bootstrapEmail.isBlank()) {
            return;
        }
        String email = bootstrapEmail.trim().toLowerCase();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || user.getRole() != null) {
            return;
        }
        user.setRole(superAdmin);
        userRepository.save(user);
    }
}
