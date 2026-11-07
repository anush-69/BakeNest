package com.bakery.bake_nest.service;

import com.bakery.bake_nest.dto.RoleRequestDTO;
import com.bakery.bake_nest.dto.RoleResponseDTO;

import java.util.List;

public interface RoleService {
    RoleResponseDTO createRole(RoleRequestDTO request);

    List<RoleResponseDTO> getAllRoles();

    RoleResponseDTO getRoleById(Long id);

    RoleResponseDTO updateRole(Long id, RoleRequestDTO request);

    void deleteRole(Long id);
}
