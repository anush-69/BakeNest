package com.bakery.bake_nest.admin.controller;

import com.bakery.bake_nest.admin.service.AdminService;
import com.bakery.bake_nest.user.dto.AccountRequests;
import com.bakery.bake_nest.user.dto.UserResponse;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admins")
public class AdminController {
    private final AdminService admins;

    public AdminController(AdminService admins) {
        this.admins = admins;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody AccountRequests.Registration request) {
        return admins.create(request);
    }

    @GetMapping
    public List<UserResponse> all() {
        return admins.all();
    }

    @GetMapping("/{id}")
    public UserResponse find(@PathVariable Long id) {
        return admins.find(id);
    }

    @PutMapping("/{id}")
    public UserResponse update(
            @PathVariable Long id, @Valid @RequestBody AccountRequests.Profile request) {
        return admins.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        admins.delete(id);
    }
}
