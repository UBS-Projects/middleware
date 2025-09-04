package com.middleware.backend.users.Roles.service;

import com.middleware.backend.users.Roles.dto.RoleRequest;
import com.middleware.backend.users.Roles.mapper.RoleMapper;
import com.middleware.backend.users.Roles.model.Role;
import com.middleware.backend.users.Roles.repository.RoleRepository;
import com.middleware.backend.users.model.User;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

    public ResponseEntity<Page<?>> getAll(Specification<Role> spec, Pageable pageable) {
        return ResponseEntity.ok(
                repo.findAll(spec,pageable));

    }

    public ResponseEntity<?> addNewRole(RoleRequest role) {
        Optional<Role> exists = repo.findByRoleName(role.getRoleName().toUpperCase());
        if (exists.isPresent()) {
            return ResponseEntity.badRequest().body("FOUND");
        }

        Role roleEntity = RoleMapper.mapToEntity(role);
        roleEntity.setId(null);
        Role saved = repo.save(roleEntity);
        return ResponseEntity.ok(RoleMapper.mapToDto(saved));
    }

}
