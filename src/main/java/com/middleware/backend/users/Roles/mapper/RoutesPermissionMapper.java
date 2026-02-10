package com.middleware.backend.users.Roles.mapper;

import com.middleware.backend.users.Roles.dto.RoleRequest;
import com.middleware.backend.users.Roles.dto.RoutesPermissionsDto;
import com.middleware.backend.users.Roles.model.Role;
import com.middleware.backend.users.Roles.model.RoutesPermissions;

/**
 * Mapper for converting between RoutesPermissions entity and DTO.
 */
public class RoutesPermissionMapper {
        /**
         * Maps a DTO to the entity, projecting roles by id and name only.
         * Note: roles field removed from entity to prevent N+1 queries.
         */
        public static RoutesPermissions mapToEntity(RoutesPermissionsDto route){
        return RoutesPermissions.builder()
                .id(route.getId())
                .routeId(route.getRouteId())
                .build();
    }

        /**
         * Maps the entity to a DTO, exposing basic role fields.
         * Roles must be fetched separately from RoleRepository.
         */
        public static RoutesPermissionsDto mapToDto(RoutesPermissions route){
        return RoutesPermissionsDto.builder()
                .id(route.getId())
                .routeId(route.getRouteId())
                .roles(java.util.Collections.emptyList())
                .build();
    }
}
