package com.bakery.bake_nest.repository;

import com.bakery.bake_nest.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

// Provide database operations for Role records
public interface RoleRepositorys extends JpaRepository<Role, Long> {

    java.util.Optional<Role> findByNameIgnoreCase(String name);

    // Lock the shared admin role while checking the last active administrator.
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from Role r where upper(r.name) = 'ADMIN'")
    java.util.Optional<Role> lockAdminRole();

    // Check for an existing name without considering letter case
    boolean existsByNameIgnoreCase(String name);

    // Check for a duplicate name while excluding the role being edited
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
