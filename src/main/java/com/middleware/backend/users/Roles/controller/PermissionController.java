package com.middleware.backend.users.Roles.controller;

import com.middleware.backend.users.Roles.dto.PermissionEditDto;
import com.middleware.backend.users.Roles.model.Permission;
import com.middleware.backend.users.Roles.service.PermissionService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/permission")
@AllArgsConstructor
public class PermissionController {
    private final PermissionService service;

    @GetMapping
    @PreAuthorize("hasAuthority('userPermissions:view')")
    public ResponseEntity<?> getAll(){
        return service.getAll();
    }
    @PatchMapping("/{roleName}")
    @PreAuthorize("hasAuthority('userPermissions:edit')")
    public ResponseEntity<?> editPermissions(@PathVariable String roleName,
                                             @RequestBody PermissionEditDto body){
        return service.editPermissions(roleName,body);
    }
}
