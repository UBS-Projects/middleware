package com.middleware.backend.users.Roles.repository;

import com.middleware.backend.users.Roles.model.Permission;
import com.middleware.backend.users.Roles.model.RoutesPermissions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoutesPermissionsRepository extends JpaRepository<RoutesPermissions,Long> {
    List<RoutesPermissions> findByRouteIdIn(List<String> names);
    List<RoutesPermissions> findAllByRouteId(String routeId);
    Optional<RoutesPermissions> findByRouteId(String routeId);
}
