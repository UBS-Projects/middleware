package com.middleware.backend.users.Roles.controller;

import com.middleware.backend.users.Roles.dto.PermissionEditDto;
import com.middleware.backend.users.Roles.model.Permission;
import com.middleware.backend.users.Roles.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints for managing permissions assigned to roles.
 * <p>
 * Supports listing all permissions and updating a specific role's permissions.
 */
@RestController
@RequestMapping("/api/permission")
@AllArgsConstructor
public class PermissionController {
    private final PermissionService service;

    @GetMapping
    @PreAuthorize("hasAuthority('userPermissions:view')")
    @Operation(
            summary = "List all permissions",
            description = "Retrieves a list of all permissions assigned to roles. Requires 'userPermissions:view' authority."
    )
    /**
     * Lists all permissions.
     * @return list of permissions
     */
    public ResponseEntity<?> getAll(){
        return service.getAll();
    }
    @PatchMapping("/{roleName}")
    @PreAuthorize("hasAuthority('userPermissions:edit')")
    @Operation(
            summary = "Edit permissions for a role",
            description = "Updates the permissions assigned to a specific role. Requires 'userPermissions:edit' authority."
    )
    /**
     * Updates the permission set for the given role.
     * @param roleName target role name
     * @param body payload containing the new permission names
     * @return update result
     */
    public ResponseEntity<?> editPermissions(@PathVariable String roleName,
                                             @RequestBody PermissionEditDto body){
        return service.editPermissions(roleName,body);
    }
}
