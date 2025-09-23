package com.middleware.backend.users.Roles.repository;

import com.middleware.backend.users.Roles.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing {@link Permission} entities.
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission,Long> {
    /**
     * Finds all permissions assigned to a given role name.
     */
    Optional<List<Permission>> findAllByRoles_RoleName(String roleName);

    /**
     * Finds permissions by a set of names.
     */
    List<Permission> findByNameIn(List<String> names);
}
