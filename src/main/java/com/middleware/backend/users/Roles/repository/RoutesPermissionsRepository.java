package com.middleware.backend.users.Roles.repository;

import com.middleware.backend.users.Roles.model.Permission;
import com.middleware.backend.users.Roles.model.RoutesPermissions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link RoutesPermissions} records mapping route IDs to roles.
 */
@Repository
public interface RoutesPermissionsRepository extends JpaRepository<RoutesPermissions,Long> {
    /**
     * Finds all route permission entries matching any of the provided route IDs.
     */
    List<RoutesPermissions> findByRouteIdIn(List<String> names);

    /**
     * Finds all entries for a specific route ID.
     */
    List<RoutesPermissions> findAllByRouteId(String routeId);

    /**
     * Retrieves a single route permission entry by its route ID.
     */
    Optional<RoutesPermissions> findByRouteId(String routeId);
}
