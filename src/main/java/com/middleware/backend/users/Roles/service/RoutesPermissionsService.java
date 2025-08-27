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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class RoutesPermissionsService {
    private final RoutesPermissionsRepository repo;
    private final RoleRepository roleRepo;




    public void save(RoutesPermissionsRequest body){
        RoutesPermissions entity = RoutesPermissions.builder()
                .routeId(body.getRouteId())
                .build();
        repo.save(entity);
    }

    public ResponseEntity<?> getAll(){
        return ResponseEntity.ok(repo.findAll().stream().map(
                RoutesPermissionMapper::mapToDto
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
        List<RoutesPermissions> permissions = repo.findByRouteIdIn(permissionNames);
        role.setRoutesPermissions(permissions);
        roleRepo.save(role);
        return ResponseEntity.ok("Permissions updated successfully");
    }

    public Set<String> getRolesForRoute(String routeId) {
        return repo.findAllByRouteId(routeId).stream().map(
                r-> r.getRouteId()
        ).collect(Collectors.toSet());
    }
}
