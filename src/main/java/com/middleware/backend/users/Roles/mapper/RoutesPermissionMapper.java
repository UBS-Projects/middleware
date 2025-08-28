package com.middleware.backend.users.Roles.mapper;

import com.middleware.backend.users.Roles.dto.RoleRequest;
import com.middleware.backend.users.Roles.dto.RoutesPermissionsDto;
import com.middleware.backend.users.Roles.model.Role;
import com.middleware.backend.users.Roles.model.RoutesPermissions;

public class RoutesPermissionMapper {
    public static RoutesPermissions mapToEntity(RoutesPermissionsDto route){
        return RoutesPermissions.builder()
                .id(route.getId())
                .routeId(route.getRouteId())
                .roles(route.getRoles().stream().map(
                                r1 -> Role.builder()
                                        .id(r1.getId())
                                        .roleName(r1.getRoleName())
                                        .build()
                        ).toList())
                .build();
    }

    public static RoutesPermissionsDto mapToDto(RoutesPermissions route){
        return RoutesPermissionsDto.builder()
                .id(route.getId())
                .routeId(route.getRouteId())
                .roles(route.getRoles().stream().map(
                        r1 -> RoleRequest.builder()
                                .id(r1.getId())
                                .roleName(r1.getRoleName())
                                .build()
                ).toList())
                .build();
    }
}
