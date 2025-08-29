package com.middleware.backend.users.Roles.controller;


import com.middleware.backend.users.Roles.dto.RoleRequest;
import com.middleware.backend.users.Roles.service.RoleService;
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
    public ResponseEntity<?> getAllRoles(){
        return service.getAll();
    }

    @PostMapping("")
    @PreAuthorize("hasAuthority('role:create')")
    public ResponseEntity<?> addNewRole(@RequestBody RoleRequest role){
        return  service.addNewRole(role);
    }


}
