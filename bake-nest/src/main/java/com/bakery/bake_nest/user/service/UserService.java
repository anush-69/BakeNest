package com.bakery.bake_nest.user.service;

import com.bakery.bake_nest.model.Role;
import com.bakery.bake_nest.model.User;
import com.bakery.bake_nest.repository.RoleRepositorys;
import com.bakery.bake_nest.repository.UserRepository;
import com.bakery.bake_nest.shared.security.CurrentAccount;
import com.bakery.bake_nest.user.dto.AccountRequests;
import com.bakery.bake_nest.user.dto.UserResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class UserService {
    private final UserRepository users;
    private final RoleRepositorys roles;
    private final PasswordEncoder passwords;
    private final CurrentAccount current;

    public UserService(
            UserRepository users,
            RoleRepositorys roles,
            PasswordEncoder passwords,
            CurrentAccount current) {
        this.users = users;
        this.roles = roles;
        this.passwords = passwords;
        this.current = current;
    }

    public UserResponse register(AccountRequests.Registration request) {
        return create(request, "CUSTOMER");
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse createAdmin(AccountRequests.Registration request) {
        return create(request, "ADMIN");
    }

    private UserResponse create(AccountRequests.Registration request, String roleName) {
        if (users.existsByUsernameIgnoreCase(request.getUsername())
                || users.existsByEmailIgnoreCase(request.getEmail().trim()))
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Username or email already exists.");
        validatePassword(request.getPassword());
        User user = new User();
        user.setUsername(request.getUsername());
        copyProfile(user, request);
        // BCrypt stores a one-way hash, never the original password.
        user.setPasswordHash(passwords.encode(request.getPassword()));
        Role role =
                roles.findByNameIgnoreCase(roleName)
                        .orElseGet(() -> {
                            Role created = new Role();
                            created.setName(roleName);
                            return roles.save(created);
                        });
        user.getRoles().add(role);
        return new UserResponse(users.save(user));
    }

    public UserResponse me() {
        return new UserResponse(current.get());
    }

    public UserResponse find(Long id) {
        current.requireOwnerOrAdmin(id);
        return new UserResponse(requireUser(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> all() {
        return users.findAll().stream().map(UserResponse::new).toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse byUsername(String username) {
        return new UserResponse(
                users.findByUsernameIgnoreCase(username).orElseThrow(() -> missing()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse byEmail(String email) {
        return new UserResponse(users.findByEmailIgnoreCase(email).orElseThrow(() -> missing()));
    }

    public UserResponse update(Long id, AccountRequests.Profile request) {
        current.requireOwnerOrAdmin(id);
        User user = requireUser(id);
        if (users.existsByEmailIgnoreCaseAndIdNot(request.getEmail().trim(), id))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists.");
        copyProfile(user, request);
        return new UserResponse(user);
    }

    public void changePassword(AccountRequests.PasswordChange request) {
        User user = current.get();
        if (!passwords.matches(request.getCurrentPassword(), user.getPasswordHash()))
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Current password is incorrect.");
        validatePassword(request.getNewPassword());
        user.setPasswordHash(passwords.encode(request.getNewPassword()));
    }

    public void deleteCustomer(Long id) {
        current.requireOwnerOrAdmin(id);
        User user = requireUser(id);
        if (current.isAdmin(user))
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Use admin management for this account.");
        // Deactivate instead of removing orders, reviews or cake booking relationships.
        user.setStatus("DELETED");
    }

    public User requireUser(Long id) {
        return users.findById(id).orElseThrow(() -> missing());
    }

    private ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found.");
    }

    private void copyProfile(User user, AccountRequests.Profile request) {
        user.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setPhone(request.getPhone());
    }

    private void validatePassword(String password) {
        // BCrypt accepts at most 72 bytes, including multi-byte characters.
        if (password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Password must fit within 72 UTF-8 bytes.");
    }
}
