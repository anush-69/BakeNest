package com.bakery.bake_nest.user.controller;

import com.bakery.bake_nest.user.dto.AccountRequests;
import com.bakery.bake_nest.user.dto.UserResponse;
import com.bakery.bake_nest.user.service.UserService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody AccountRequests.Registration request) {
        return users.register(request);
    }

    @GetMapping("/me")
    public UserResponse me() {
        return users.me();
    }

    @GetMapping
    public List<UserResponse> all() {
        return users.all();
    }

    @GetMapping("/{id}")
    public UserResponse find(@PathVariable Long id) {
        return users.find(id);
    }

    @GetMapping("/by-username")
    public UserResponse byUsername(@RequestParam String username) {
        return users.byUsername(username);
    }

    @GetMapping("/by-email")
    public UserResponse byEmail(@RequestParam String email) {
        return users.byEmail(email);
    }

    @PutMapping("/{id}")
    public UserResponse update(
            @PathVariable Long id, @Valid @RequestBody AccountRequests.Profile request) {
        return users.update(id, request);
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void password(@Valid @RequestBody AccountRequests.PasswordChange request) {
        users.changePassword(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        users.deleteCustomer(id);
    }
}
