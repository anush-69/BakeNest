package com.bakery.bake_nest.admin.service;

import com.bakery.bake_nest.model.Role;
import com.bakery.bake_nest.model.User;
import com.bakery.bake_nest.repository.RoleRepositorys;
import com.bakery.bake_nest.repository.UserRepository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

// Optional local migration. There is deliberately no public HTTP endpoint.
@Service
public class LegacyAdminImporter {
    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final RoleRepositorys roles;

    public LegacyAdminImporter(JdbcTemplate jdbc, UserRepository users, RoleRepositorys roles) {
        this.jdbc = jdbc;
        this.users = users;
        this.roles = roles;
    }

    @Transactional
    public int importAdmins() {
        int imported = 0;
        // Only legacy full administrators may receive the application's ADMIN role.
        var records =
                jdbc.queryForList(
                        "SELECT id, email, full_name, password_hash FROM admins WHERE role IN"
                            + " ('SUPER_ADMIN', 'ADMIN')");
        for (var record : records) {
            String username = "legacy-admin-" + record.get("id");
            String email = ((String) record.get("email")).trim().toLowerCase(Locale.ROOT);
            var existing = users.findByUsernameIgnoreCase(username);
            if (existing.isPresent()
                    && existing.get().getEmail().equalsIgnoreCase(email)
                    && existing.get().getRoles().stream()
                            .anyMatch(role -> "ADMIN".equalsIgnoreCase(role.getName()))) {
                continue; // Running the import twice must not create duplicate accounts.
            }
            if (existing.isPresent() || users.existsByEmailIgnoreCase(email)) {
                throw new IllegalStateException(
                        "Legacy import stopped: an account name or email is already in use. No"
                            + " existing account was promoted.");
            }
            String hash = (String) record.get("password_hash");
            if (hash == null || !hash.matches("\\$2[aby]\\$[0-9]{2}\\$[./A-Za-z0-9]{53}")) {
                throw new IllegalStateException(
                        "Legacy import requires valid BCrypt password hashes.");
            }
            Role role =
                    roles.findByNameIgnoreCase("ADMIN")
                            .orElseGet(() -> {
                                Role created = new Role();
                                created.setName("ADMIN");
                                return roles.save(created);
                            });
            User admin = new User();
            admin.setUsername(username);
            admin.setEmail(email);
            String fullName = (String) record.get("full_name");
            String[] names =
                    fullName == null ? new String[] {"Admin"} : fullName.trim().split("\\s+", 2);
            admin.setFirstName(names[0]);
            admin.setLastName(names.length > 1 ? names[1] : "");
            admin.setPasswordHash(
                    hash); // Reuse the existing BCrypt hash without knowing the password.
            admin.getRoles().add(role);
            users.save(admin);
            imported++;
        }
        // The legacy admins table is never updated or deleted.
        users.flush();
        return imported;
    }
}
