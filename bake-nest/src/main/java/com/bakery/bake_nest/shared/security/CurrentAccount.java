package com.bakery.bake_nest.shared.security;

import com.bakery.bake_nest.model.User;
import com.bakery.bake_nest.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CurrentAccount {
    private final UserRepository users;

    public CurrentAccount(UserRepository users) {
        this.users = users;
    }

    public User get() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in.");
        User user =
                users.findByUsernameIgnoreCase(authentication.getName())
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED, "Please log in."));
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account inactive.");
        return user;
    }

    public boolean isAdmin(User user) {
        return user.getRoles().stream().anyMatch(role -> "ADMIN".equalsIgnoreCase(role.getName()));
    }

    public void requireOwner(Long ownerId) {
        if (!get().getId().equals(ownerId))
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "This record belongs to another customer.");
    }

    public void requireOwnerOrAdmin(Long ownerId) {
        User user = get();
        if (!user.getId().equals(ownerId) && !isAdmin(user))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
    }
}
