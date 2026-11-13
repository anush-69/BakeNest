package com.bakery.bake_nest;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bakery.bake_nest.admin.service.LegacyAdminImporter;
import com.bakery.bake_nest.model.Role;
import com.bakery.bake_nest.model.User;
import com.bakery.bake_nest.repository.RoleRepositorys;
import com.bakery.bake_nest.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

class LegacyAdminImporterTests {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final UserRepository users = mock(UserRepository.class);
    private final RoleRepositorys roles = mock(RoleRepositorys.class);
    private final LegacyAdminImporter importer = new LegacyAdminImporter(jdbc, users, roles);
    private String hash;
    private Role role;

    @BeforeEach
    void setup() {
        hash = new BCryptPasswordEncoder().encode("ExistingPassword!");
        role = new Role();
        role.setName("ADMIN");
        when(jdbc.queryForList(anyString()))
                .thenReturn(
                        List.of(
                                Map.of(
                                        "id",
                                        1L,
                                        "email",
                                        "legacy@example.com",
                                        "full_name",
                                        "Existing Admin",
                                        "password_hash",
                                        hash)));
        when(roles.findByNameIgnoreCase("ADMIN")).thenReturn(Optional.of(role));
    }

    @Test
    void preservesHashAndDoesNotWriteToLegacyTable() {
        assertThat(importer.importAdmins()).isEqualTo(1);
        var saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("legacy-admin-1");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo(hash);
        assertThat(saved.getValue().getRoles()).contains(role);
        verify(jdbc)
                .queryForList(
                        "SELECT id, email, full_name, password_hash FROM admins WHERE role IN"
                            + " ('SUPER_ADMIN', 'ADMIN')");
        verifyNoMoreInteractions(jdbc);
    }

    @Test
    void neverPromotesAnExistingCustomerWithSameEmail() {
        when(users.existsByEmailIgnoreCase("legacy@example.com")).thenReturn(true);
        assertThatThrownBy(importer::importAdmins).isInstanceOf(IllegalStateException.class);
        verify(users, never()).save(any());
    }

    @Test
    void repeatedImportDoesNotDuplicateAccounts() {
        User existing = new User();
        existing.setEmail("legacy@example.com");
        existing.getRoles().add(role);
        when(users.findByUsernameIgnoreCase("legacy-admin-1")).thenReturn(Optional.of(existing));
        assertThat(importer.importAdmins()).isZero();
        verify(users, never()).save(any());
    }

    @Test
    void refusesPlainTextPasswords() {
        when(jdbc.queryForList(anyString()))
                .thenReturn(
                        List.of(
                                Map.of(
                                        "id",
                                        1L,
                                        "email",
                                        "legacy@example.com",
                                        "full_name",
                                        "Admin",
                                        "password_hash",
                                        "plain-password")));
        assertThatThrownBy(importer::importAdmins).isInstanceOf(IllegalStateException.class);
        verify(users, never()).save(any());
    }
}
