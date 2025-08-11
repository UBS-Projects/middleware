package com.middleware.backend.users.Roles.service;

import com.middleware.backend.users.Roles.dto.RoleRequest;
import com.middleware.backend.users.Roles.mapper.RoleMapper;
import com.middleware.backend.users.Roles.model.Role;
import com.middleware.backend.users.Roles.repository.RoleRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class RoleService {
    private final RoleRepository repo;


    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(
                repo.findAll()
                        .stream()
                        .map(RoleMapper::mapToDto)
                        .collect(Collectors.toList())
        );

    }

    public ResponseEntity<?> addNewRole(RoleRequest role) {
        Optional<Role> exists = repo.findByRoleName(role.getRoleName().toUpperCase());
        if (exists.isPresent()) {
            return ResponseEntity.badRequest().body("FOUND");
        }

        Role roleEntity = RoleMapper.mapToEntity(role);
        roleEntity.setId(null);
        roleEntity.setRoleName(roleEntity.getRoleName().toUpperCase());
        Role saved = repo.save(roleEntity);
        return ResponseEntity.ok(RoleMapper.mapToDto(saved));
    }

}
