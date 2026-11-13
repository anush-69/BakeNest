package com.bakery.bake_nest.security;

import com.bakery.bake_nest.model.User;
import com.bakery.bake_nest.repository.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("booking-local")
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DatabaseUserPrincipal loadUserByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw unavailable();
        }
        User user = userRepository.findByUsername(username).orElseThrow(this::unavailable);
        // Enforce exact matching even with a case-insensitive database collation.
        if (!username.equals(user.getUsername()) || user.getId() == null || user.getId() <= 0) {
            throw unavailable();
        }
        String hash = user.getPasswordHash();
        return new DatabaseUserPrincipal(user.getId(), user.getUsername(),
                hash == null ? "" : hash, "ACTIVE".equals(user.getStatus()));
    }

    private UsernameNotFoundException unavailable() {
        return new UsernameNotFoundException("Invalid credentials.");
    }
}
