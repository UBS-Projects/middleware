package com.middleware.backend.users.Roles.controller;

import com.middleware.backend.users.Roles.dto.PermissionEditDto;
import com.middleware.backend.users.Roles.service.RoutesPermissionsService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints for managing route-level permissions mapped to roles.
 */
@RestController
@RequestMapping("/api/routespermissions")
@AllArgsConstructor
public class RoutesPermissionsController {
    private final RoutesPermissionsService service;

    @GetMapping("")
    @PreAuthorize("hasAuthority('routePermissions:view')")
    @Operation(
            summary = "List all route permissions",
            description = "Retrieves a list of all route permissions assigned to roles. Requires 'routePermissions:view' authority."
    )
    /**
     * Lists all route permissions across roles.
     */
    public ResponseEntity<?> getAll(){
        return service.getAll();
    }

    @PatchMapping("/{roleName}")
    @PreAuthorize("hasAuthority('routePermissions:edit')")
    @Operation(
            summary = "Edit route permissions for a role",
            description = "Updates the route permissions assigned to a specific role. Requires 'routePermissions:edit' authority."
    )
    /**
     * Updates route permissions of a given role.
     * @param roleName role to update
     * @param body payload containing route permission identifiers
     */
    public ResponseEntity<?> editPermissions(@PathVariable String roleName,
                                             @RequestBody PermissionEditDto body){
        return service.editPermissions(roleName,body);
    }

}
