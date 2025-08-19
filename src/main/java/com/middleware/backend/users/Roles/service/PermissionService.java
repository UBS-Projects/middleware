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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PermissionService {
    private final PermissionRepository repo;
    private final RoleRepository roleRepo;

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
    public ResponseEntity<?> editPermissions(String roleName, PermissionEditDto body) {

        Optional<Role> optionalRole = roleRepo.findByRoleName(roleName);
        if(optionalRole.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Role not found");
        }
        Role role = optionalRole.get();

        List<String> permissionNames = body.getPermissions();

        List<Permission> permissions = repo.findByNameIn(permissionNames);

        role.setPermissions(permissions);

        roleRepo.save(role);

        return ResponseEntity.ok("Permissions updated successfully");
    }

}
