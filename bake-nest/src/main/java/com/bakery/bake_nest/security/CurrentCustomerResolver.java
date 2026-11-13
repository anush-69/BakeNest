package com.bakery.bake_nest.security;

import com.bakery.bake_nest.model.User;
import com.bakery.bake_nest.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CurrentCustomerResolver {
    private final UserRepository userRepository;

    public CurrentCustomerResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User resolve(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails)) {
            throw new AuthenticationCredentialsNotFoundException("Database authentication required.");
        }
        // Reload identity and status instead of trusting stale session data.
        User customer = userRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Customer unavailable."));
        if (!"ACTIVE".equalsIgnoreCase(customer.getStatus())) {
            throw new AccessDeniedException("Customer unavailable.");
        }
        return customer;
    }
}
