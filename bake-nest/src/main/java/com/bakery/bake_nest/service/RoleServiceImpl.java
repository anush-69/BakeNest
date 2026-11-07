package com.bakery.bake_nest.service;

import com.bakery.bake_nest.dto.RoleRequestDTO;
import com.bakery.bake_nest.dto.RoleResponseDTO;
import com.bakery.bake_nest.model.Role;
import com.bakery.bake_nest.repository.RoleRepositorys;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Tell Spring that this class contains the role business logic
@Service
@org.springframework.transaction.annotation.Transactional
@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
public class RoleServiceImpl implements RoleService {

    // Store the repository used to access the database
    private final RoleRepositorys roleRepositorys;
    private final com.bakery.bake_nest.repository.UserRepository userRepository;

    // Receive the repository from Spring
    public RoleServiceImpl(RoleRepositorys roleRepositorys, com.bakery.bake_nest.repository.UserRepository userRepository) {
        this.roleRepositorys = roleRepositorys;
        this.userRepository = userRepository;
    }

    // Create a new role
    @Override
    public RoleResponseDTO createRole(RoleRequestDTO request) {
        // Check the name and remove extra spaces
        String name = validateRoleName(request);

        // Reject an existing name, including differences in letter case
        if (roleRepositorys.existsByNameIgnoreCase(name)) {
            // Return HTTP 409 to indicate a duplicate
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A role with this name already exists."
            );
        }

        // Create an empty Role object
        Role role = new Role();

        // Set the validated role name
        role.setName(name);

        // Save the role in the database
        Role savedRole = roleRepositorys.save(role);

        // Return the saved role details
        return convertToDTO(savedRole);
    }

    // Get all existing roles
    @Override
    public List<RoleResponseDTO> getAllRoles() {
        // Read all roles from the database
        List<Role> roles = roleRepositorys.findAll();

        // Create a list for the response objects
        List<RoleResponseDTO> responses = new ArrayList<>();

        // Process each role
        for (Role role : roles) {
            // Convert the role and add it to the response list
            responses.add(convertToDTO(role));
        }

        // Return all role responses
        return responses;
    }

    // Get one role using its ID
    @Override
    public RoleResponseDTO getRoleById(Long id) {
        // Search for the role in the database
        Optional<Role> optionalRole = roleRepositorys.findById(id);

        // Check whether the role is missing
        if (!optionalRole.isPresent()) {
            // Return HTTP 404 when the role does not exist
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Role not found."
            );
        }

        // Convert the existing role into a response
        return convertToDTO(optionalRole.get());
    }

    // Update an existing role
    @Override
    public RoleResponseDTO updateRole(Long id, RoleRequestDTO request) {
        // Search for the selected role
        Optional<Role> optionalRole = roleRepositorys.findById(id);

        // Check whether the selected role is missing
        if (!optionalRole.isPresent()) {
            // Return HTTP 404 when the role does not exist
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Role not found."
            );
        }

        // Check the new name and remove extra spaces
        String name = validateRoleName(request);

        // Check whether another role already uses this name
        if (roleRepositorys.existsByNameIgnoreCaseAndIdNot(name, id)) {
            // Return HTTP 409 for a duplicate name
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A role with this name already exists."
            );
        }

        // Get the existing Role object
        Role role = optionalRole.get();
        checkRoleCanChange(role);

        // Change its name while keeping the same ID
        role.setName(name);

        // Save the updated role
        Role updatedRole = roleRepositorys.save(role);

        // Return the updated role details
        return convertToDTO(updatedRole);
    }

    // Delete an existing role
    @Override
    public void deleteRole(Long id) {
        // Check whether the role exists before deleting
        if (!roleRepositorys.existsById(id)) {
            // Return HTTP 404 when the role does not exist
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Role not found."
            );
        }

        // Delete the role using its ID
        Role role = roleRepositorys.findById(id).orElseThrow();
        checkRoleCanChange(role);
        roleRepositorys.delete(role);
    }

    // Validate the name for both creating and updating
    private String validateRoleName(RoleRequestDTO request) {
        // Reject a missing request or missing name
        if (request == null || request.getName() == null) {
            // Return HTTP 400 for invalid input
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Role name is required."
            );
        }

        // Remove spaces from the beginning and end
        String name = request.getName().trim().toUpperCase(java.util.Locale.ROOT);

        // Reject an empty name or a name containing only spaces
        if (!name.matches("[A-Z][A-Z0-9_]{1,49}") || name.startsWith("ROLE_")) {
            // Return HTTP 400 for an empty name
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Use 2-50 uppercase letters, digits or underscores, without ROLE_."
            );
        }

        // Return the cleaned name
        return name;
    }

    private void checkRoleCanChange(Role role) {
        // Built-in and assigned roles must keep their meaning for authorization.
        if ("ADMIN".equalsIgnoreCase(role.getName()) || "CUSTOMER".equalsIgnoreCase(role.getName())
                || userRepository.existsByRolesId(role.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Built-in or assigned roles cannot be renamed or deleted.");
        }
    }

    // Convert a Role entity into the frontend response format
    private RoleResponseDTO convertToDTO(Role role) {
        // Include only the role ID and name
        return new RoleResponseDTO(
                role.getId(),
                role.getName()
        );
    }
}
