package com.middleware.backend.users.Roles.controller;


import com.middleware.backend.users.Roles.dto.RoleRequest;
import com.middleware.backend.users.Roles.model.Role;
import com.middleware.backend.users.Roles.service.RoleService;
import com.middleware.backend.users.Roles.specification.RoleSpecification;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Endpoints for querying and managing roles.
 */
@RestController
@RequestMapping("/api/role")
@AllArgsConstructor
public class RoleController {
    private final RoleService service;
    @GetMapping("/user")
    @PreAuthorize("hasAuthority('role:view')")
    @Operation(
            summary = "List roles",
            description = "Retrieves a list of all roles available in the system. Requires 'role:view' authority."
    )
        /**
         * Returns all roles targeted for user assignment.
         */
        public ResponseEntity<?> getAllRolesForUsers(){
        return service.getAll();
    }
    @GetMapping("")
    @PreAuthorize("hasAuthority('role:view')")
    @Operation(
            summary = "List roles",
            description = "Retrieves a list of all roles available in the system. Requires 'role:view' authority."
    )
    /**
     * Returns a paginated list of roles with optional filters.
     * @param roleName contains filter on role name
     * @param roleType exact enum filter on role type
     * @param sortedBy property to sort by
     * @param sortDirection asc or desc
     * @param page page index
     * @param size page size
     */
    public ResponseEntity<Page<?>> getAllRoles(
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String roleType,
            @RequestParam(required = false, defaultValue = "updatedAt") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        Role.RoleType roleTypeEnum = null;
        if (roleType != null && !roleType.isEmpty()) {
                roleTypeEnum = Role.RoleType.valueOf(roleType);
        }
        Pageable pageable = PageRequest.of(page, size, sortDirection.equalsIgnoreCase("asc")
                ? Sort.by(sortedBy).ascending()
                : Sort.by(sortedBy).descending());
        Specification<Role> spec = Specification
                .where(RoleSpecification.hasField("roleName", roleName, RoleSpecification.MatchMode.CONTAINS))
                .and(roleTypeEnum != null
                ? RoleSpecification.hasFieldEnum("roleType", roleTypeEnum)
                : null);;
        return service.getAll(spec,pageable);
    }

    @PostMapping("")
    @PreAuthorize("hasAuthority('role:create')")
    @Operation(
            summary = "Create new role",
            description = "Creates a new role with the provided details. Requires 'role:create' authority."
    )
        /**
         * Creates a new role.
         */
        public ResponseEntity<?> addNewRole(@RequestBody RoleRequest role){
        return  service.addNewRole(role);
    }


}
