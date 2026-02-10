package com.middleware.backend.users.Roles.repository;

import com.middleware.backend.users.Roles.model.Role;
import com.middleware.backend.users.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing {@link Role} entities.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role,Long> {
    /**
     * Retrieves a role by its unique name.
     */
    Optional<Role> findByRoleName(String name);

    /**
     * Finds all roles that have a specific permission.
     */
    List<Role> findByPermissions_Id(Long permissionId);

    /**
     * Finds all roles that have a specific routes permission.
     */
    List<Role> findByRoutesPermissions_Id(Long routesPermissionId);

    /**
     * Finds roles by specification with pagination and sorting.
     */
    Page<Role> findAll(Specification<Role> spec, Pageable pageable);
}
