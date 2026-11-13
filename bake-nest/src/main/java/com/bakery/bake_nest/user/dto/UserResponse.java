package com.bakery.bake_nest.user.dto;

import com.bakery.bake_nest.model.User;

import lombok.Getter;

import java.util.List;

@Getter
public class UserResponse {
    private final Long id;
    private final String username, email, firstName, lastName, phone, status;
    private final List<String> roles;

    public UserResponse(User user) {
        id = user.getId();
        username = user.getUsername();
        email = user.getEmail();
        firstName = user.getFirstName();
        lastName = user.getLastName();
        phone = user.getPhone();
        status = user.getStatus();
        roles = user.getRoles().stream().map(role -> role.getName()).sorted().toList();
        // Password hashes are deliberately absent from this response.
    }
}
