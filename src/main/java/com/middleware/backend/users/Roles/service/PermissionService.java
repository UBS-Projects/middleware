package com.middleware.backend.users.Roles.service;

import com.middleware.backend.users.Roles.dto.PermissionEditDto;
import com.middleware.backend.users.Roles.dto.PermissionRequestDTO;
import com.middleware.backend.users.Roles.dto.PermissionsDTO;
import com.middleware.backend.users.Roles.dto.RoleRequest;
import com.middleware.backend.users.Roles.model.Permission;
import com.middleware.backend.users.Roles.model.Role;
import com.middleware.backend.users.Roles.repository.PermissionRepository;
import com.middleware.backend.users.Roles.repository.RoleRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Service for viewing and editing permissions assigned to roles.
 */
@Service
@AllArgsConstructor
public class PermissionService {
    private final PermissionRepository repo;
    private final RoleRepository roleRepo;

    /**
     * Retrieve all permissions with their associated roles, mapped to DTOs.
     * @return HTTP 200 containing a stream of {@link PermissionsDTO}
     */
    public ResponseEntity<?> getAll() {
        List<Permission> pers = repo.findAll();
        return ResponseEntity.ok(pers.stream().map(
                per -> PermissionsDTO.builder()
                        .id(per.getId())
                        .name(per.getName())
                        .roles(per.getRoles().stream().map(
                                r -> RoleRequest.builder()
                                        .id(r.getId())
                                        .roleName(r.getRoleName())
                                        .build()
                        ).toList()).build()
        ));
    }

    @Transactional
    /**
     * Replace the set of basic permissions for a given role.
     * Updates audit fields (updatedBy/updatedAt) from the authenticated user.
     * @param roleName role to modify
     * @param body request containing permission names
     * @return 404 if role not found; 200 on success
     */
    public ResponseEntity<?> editPermissions(String roleName, PermissionEditDto body) {

        Optional<Role> optionalRole = roleRepo.findByRoleName(roleName);
        if(optionalRole.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Role not found");
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUser = authentication.getName();
        Role role = optionalRole.get();

        List<String> permissionNames = body.getPermissions();

        List<Permission> permissions = repo.findByNameIn(permissionNames);

        role.setPermissions(permissions);

        role.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        role.setUpdatedBy(emailUser);
        roleRepo.save(role);

        return ResponseEntity.ok("Permissions updated successfully");
    }

}
