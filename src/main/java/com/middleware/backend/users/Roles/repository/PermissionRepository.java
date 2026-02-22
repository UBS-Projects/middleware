package com.middleware.backend.users.Roles.repository;

import com.middleware.backend.users.Roles.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
     * Uses explicit join query since bidirectional relationship was removed for performance.
     */
    @Query("SELECT p FROM Permission p JOIN Role r ON p MEMBER OF r.permissions WHERE r.roleName = :roleName")
    Optional<List<Permission>> findAllByRoleName(@Param("roleName") String roleName);

    /**
     * Finds permissions by a set of names.
     */
    List<Permission> findByNameIn(List<String> names);
}
