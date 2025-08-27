package com.middleware.backend.users.Roles.controller;

import com.middleware.backend.users.Roles.dto.PermissionEditDto;
import com.middleware.backend.users.Roles.service.RoutesPermissionsService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/routespermissions")
@AllArgsConstructor
public class RoutesPermissionsController {
    private final RoutesPermissionsService service;

    @GetMapping("")
    public ResponseEntity<?> getAll(){
        return service.getAll();
    }

    @PatchMapping("/{roleName}")
    public ResponseEntity<?> editPermissions(@PathVariable String roleName,
                                             @RequestBody PermissionEditDto body){
        return service.editPermissions(roleName,body);
    }

}
