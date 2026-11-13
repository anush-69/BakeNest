package com.bakery.bake_nest.repository;

import com.bakery.bake_nest.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Access existing user accounts for login and role checks
public interface UserRepository extends JpaRepository<User, Long> {

    // Load the account's roles together with the account
    @EntityGraph(attributePaths = "roles")

    // Find an account by username; Optional handles a missing account
    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameIgnoreCase(String username);
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    boolean existsByRolesId(Long roleId);
    java.util.List<User> findDistinctByRolesNameIgnoreCase(String roleName);
    long countByRolesNameIgnoreCaseAndStatus(String roleName, String status);
}
