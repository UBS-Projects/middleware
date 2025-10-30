package com.middleware.backend.users.Roles.service;

import com.middleware.backend.users.Roles.dto.PermissionEditDto;
import com.middleware.backend.users.Roles.dto.RoutesPermissionsDto;
import com.middleware.backend.users.Roles.dto.RoutesPermissionsRequest;
import com.middleware.backend.users.Roles.mapper.RoutesPermissionMapper;
import com.middleware.backend.users.Roles.model.Permission;
import com.middleware.backend.users.Roles.model.Role;
import com.middleware.backend.users.Roles.model.RoutesPermissions;
import com.middleware.backend.users.Roles.repository.RoleRepository;
import com.middleware.backend.users.Roles.repository.RoutesPermissionsRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing route-based permissions associations for roles.
 */
@Service
@AllArgsConstructor
public class RoutesPermissionsService {
    private final RoutesPermissionsRepository repo;
    private final RoleRepository roleRepo;




    /**
     * Ensure a route exists in the permissions table; creates if missing.
     * @param body request containing the route identifier
     */
    public void save(RoutesPermissionsRequest body){
        Optional<RoutesPermissions> exists = repo.findByRouteId(body.getRouteId());
        if(exists.isPresent()){
            return;
        }
        RoutesPermissions entity = RoutesPermissions.builder()
                .routeId(body.getRouteId())
                .build();
        repo.save(entity);
    }

    /**
     * List all registered routes permissions as DTOs.
     * @return HTTP 200 with a stream of {@link RoutesPermissionsDto}
     */
    public ResponseEntity<?> getAll(){
        return ResponseEntity.ok(repo.findAll().stream().map(
                RoutesPermissionMapper::mapToDto
        ));
    }
    @Transactional
    /**
     * Replace the set of routes permissions attached to a role.
     * Updates audit fields from the authenticated user.
     * @param roleName role to modify
     * @param body list of route identifiers in {@link PermissionEditDto#permissions}
     * @return 404 if role not found; 200 on success
     */
    public ResponseEntity<?> editPermissions(String roleName, PermissionEditDto body) {

        Optional<Role> optionalRole = roleRepo.findByRoleName(roleName);
        if(optionalRole.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Role not found");
        }
        if(optionalRole.get().getRoleName().equals("EMAIL_SENDER")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("EMAIL_SENDER Role can't be Edited");
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUser = authentication.getName();
        Role role = optionalRole.get();
        List<String> permissionNames = body.getPermissions();
        List<RoutesPermissions> permissions = repo.findByRouteIdIn(permissionNames);
        role.setRoutesPermissions(permissions);
        role.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        role.setUpdatedBy(emailUser);
        roleRepo.save(role);
        return ResponseEntity.ok("Permissions updated successfully");
    }

    /**
     * Get the set of route ids matching the provided route (alias; returns unique ids).
     * @param routeId route identifier
     * @return set of unique route ids
     */
    public Set<String> getRolesForRoute(String routeId) {
        return repo.findAllByRouteId(routeId).stream().map(
                r-> r.getRouteId()
        ).collect(Collectors.toSet());
    }
}
