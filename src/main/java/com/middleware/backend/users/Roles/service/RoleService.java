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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class RoleService {
    private final RoleRepository repo;
    /**
     * Retrieve all roles in the system.
     * <p>
     * Results are mapped to DTOs using {@link RoleMapper#mapToDto(Role)} and returned
     * as the body of a {@link ResponseEntity}.
     * </p>
     *
     * @return {@link ResponseEntity} containing a list of role DTOs.
     */
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(
                repo.findAll()
                        .stream()
                        .map(RoleMapper::mapToDto)
                        .collect(Collectors.toList())
        );

    }

    /**
     * Retrieve a paginated list of roles matching the provided specification.
     *
     * @param spec     JPA {@link Specification} used to filter {@link Role} entities.
     * @param pageable {@link Pageable} describing page number, size, and sorting.
     * @return {@link ResponseEntity} containing a {@link Page} of roles that match the criteria.
     */
    public ResponseEntity<Page<?>> getAll(Specification<Role> spec, Pageable pageable) {
        return ResponseEntity.ok(
                repo.findAll(spec,pageable));

    }

    /**
     * Create a new role.
     * <p>
     * If a role with the same name (case-insensitive) already exists, the method returns
     * {@code 400 Bad Request} with a body of {@code "FOUND"}. Otherwise, it persists a new
     * {@link Role} entity populated from the provided {@link RoleRequest}, sets audit
     * information (createdBy/updatedBy and timestamps) from the currently authenticated user,
     * and returns the saved role mapped to a DTO.
     * </p>
     *
     * @param role the incoming request payload describing the role to create.
     * @return {@link ResponseEntity} with either an error message if duplicate, or the created role DTO.
     */
    public ResponseEntity<?> addNewRole(RoleRequest role) {
        Optional<Role> exists = repo.findByRoleName(role.getRoleName().toUpperCase());
        if (exists.isPresent()) {
            return ResponseEntity.badRequest().body("FOUND");
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUser = authentication.getName();
        Role roleEntity = RoleMapper.mapToEntity(role);
        roleEntity.setId(null);
        roleEntity.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        roleEntity.setCreatedBy(emailUser);
        roleEntity.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        roleEntity.setUpdatedBy(emailUser);
        roleEntity.setRoleName(role.getRoleName().toUpperCase());
        Role saved = repo.save(roleEntity);
        return ResponseEntity.ok(RoleMapper.mapToDto(saved));
    }

}
