package com.bakery.bake_nest.service;

import com.bakery.bake_nest.model.Role;
import com.bakery.bake_nest.model.User;
import com.bakery.bake_nest.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

// Provide database account details to Spring Security
@Service
public class AccountUserDetailsService implements UserDetailsService {

    // Store the repository used to find accounts
    private final UserRepository userRepository;

    // Receive the repository from Spring
    public AccountUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Find the account when someone attempts to log in
    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        // Search the database and reject an unknown username
        User account = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Account not found.")
                );

        // Create a list for the account's security roles
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        // Process each role assigned to this account
        for (Role role : account.getRoles()) {
            // Add Spring's ROLE_ prefix to the stored role name
            authorities.add(
                    new SimpleGrantedAuthority("ROLE_" + role.getName().toUpperCase(java.util.Locale.ROOT))
            );
            for (var permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.getCode()));
            }
        }

        // Convert the database account into Spring Security login details
        return org.springframework.security.core.userdetails.User
                // Use the account's username
                .withUsername(account.getUsername())

                // Use the stored password hash
                .password(account.getPasswordHash())

                // Include the account's assigned roles
                .authorities(authorities)

                // Allow login only when the account status is ACTIVE
                .disabled(!"ACTIVE".equalsIgnoreCase(account.getStatus()))

                // Create the completed login details
                .build();
    }
}
