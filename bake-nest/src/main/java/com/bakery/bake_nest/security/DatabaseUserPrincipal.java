package com.bakery.bake_nest.security;

import java.util.List;

public final class DatabaseUserPrincipal extends org.springframework.security.core.userdetails.User {
    private static final long serialVersionUID = 1L;
    private final Long userId;

    public DatabaseUserPrincipal(Long userId, String username, String passwordHash, boolean enabled) {
        // No roles are granted until an authority policy is defined.
        super(username, passwordHash, enabled, true, true, true, List.of());
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("A persisted user ID is required.");
        }
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}
