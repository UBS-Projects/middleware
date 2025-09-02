package com.middleware.backend.users.Roles.controller;


import com.middleware.backend.users.Roles.dto.RoleRequest;
import com.middleware.backend.users.Roles.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/role")
@AllArgsConstructor
public class RoleController {
    private final RoleService service;

    @GetMapping("")
    @PreAuthorize("hasAuthority('role:view')")
    @Operation(
            summary = "List roles",
            description = "Retrieves a list of all roles available in the system. Requires 'role:view' authority."
    )
    public ResponseEntity<?> getAllRoles(){
        return service.getAll();
    }

    @PostMapping("")
    @PreAuthorize("hasAuthority('role:create')")
    @Operation(
            summary = "Create new role",
            description = "Creates a new role with the provided details. Requires 'role:create' authority."
    )
    public ResponseEntity<?> addNewRole(@RequestBody RoleRequest role){
        return  service.addNewRole(role);
    }


}
