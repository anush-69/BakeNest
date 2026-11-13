package com.bakery.bake_nest.controller;

import com.bakery.bake_nest.dto.RoleRequestDTO;
import com.bakery.bake_nest.dto.RoleResponseDTO;
import com.bakery.bake_nest.service.RoleService;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    public RoleResponseDTO createRole(
            @RequestBody RoleRequestDTO request) {

        return roleService.createRole(request);
    }

    @GetMapping
    public List<RoleResponseDTO> getAllRoles() {

        return roleService.getAllRoles();
    }

    @GetMapping("/{id}")
    public RoleResponseDTO getRoleById(
            @PathVariable Long id) {

        return roleService.getRoleById(id);
    }

    @PutMapping("/{id}")
    public RoleResponseDTO updateRole(
            @PathVariable Long id,
            @RequestBody RoleRequestDTO request) {

        return roleService.updateRole(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteRole(@PathVariable Long id) {

        roleService.deleteRole(id);
    }
}

