package com.bakery.bake_nest.admin.service;

import com.bakery.bake_nest.repository.RoleRepositorys;
import com.bakery.bake_nest.repository.UserRepository;
import com.bakery.bake_nest.shared.security.CurrentAccount;
import com.bakery.bake_nest.user.dto.AccountRequests;
import com.bakery.bake_nest.user.dto.UserResponse;
import com.bakery.bake_nest.user.service.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
@PreAuthorize("hasRole('ADMIN')")
public class AdminService {
    private final UserService users;
    private final UserRepository repository;
    private final RoleRepositorys roles;
    private final CurrentAccount current;

    public AdminService(
            UserService users,
            UserRepository repository,
            RoleRepositorys roles,
            CurrentAccount current) {
        this.users = users;
        this.repository = repository;
        this.roles = roles;
        this.current = current;
    }

    public UserResponse create(AccountRequests.Registration request) {
        return users.createAdmin(request);
    }

    public List<UserResponse> all() {
        return repository.findDistinctByRolesNameIgnoreCase("ADMIN").stream()
                .map(UserResponse::new)
                .toList();
    }

    public UserResponse find(Long id) {
        var user = users.requireUser(id);
        if (!current.isAdmin(user))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found.");
        return new UserResponse(user);
    }

    public UserResponse update(Long id, AccountRequests.Profile request) {
        find(id);
        return users.update(id, request);
    }

    public void delete(Long id) {
        roles.lockAdminRole();
        find(id);
        var user = users.requireUser(id);
        if (current.get().getId().equals(id))
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "An admin cannot delete their own account.");
        if ("ACTIVE".equals(user.getStatus())
                && repository.countByRolesNameIgnoreCaseAndStatus("ADMIN", "ACTIVE") <= 1)
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Keep at least one active admin.");
        user.setStatus("DELETED");
    }
}
