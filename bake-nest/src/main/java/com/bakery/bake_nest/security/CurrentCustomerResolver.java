package com.bakery.bake_nest.security;

import com.bakery.bake_nest.model.User;
import com.bakery.bake_nest.repository.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("booking-local")
public class CurrentCustomerResolver {
    private final UserRepository userRepository;

    public CurrentCustomerResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User resolve(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof DatabaseUserPrincipal principal)) {
            throw new AuthenticationCredentialsNotFoundException("Database authentication required.");
        }
        // Reload identity and status instead of trusting stale session data.
        User customer = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new AccessDeniedException("Customer unavailable."));
        if (!"ACTIVE".equals(customer.getStatus())) {
            throw new AccessDeniedException("Customer unavailable.");
        }
        return customer;
    }
}
