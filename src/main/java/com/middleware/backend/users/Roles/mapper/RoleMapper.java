package com.middleware.backend.users.Roles.mapper;

import com.middleware.backend.users.Roles.dto.RoleRequest;
import com.middleware.backend.users.Roles.model.Role;

/**
 * Mapper for converting between Role domain entity and RoleRequest DTO.
 */
public class RoleMapper {
    /**
     * Maps a RoleRequest to a Role entity.
     * Interprets roleType string as USER vs SYSTEM_USER.
     */
    public static Role mapToEntity(RoleRequest role){
        return Role.builder()
                .roleName(role.getRoleName())
                .id(role.getId())
                .roleType(role.getRoleType().equals("USER")?Role.RoleType.USER:Role.RoleType.SYSTEM_USER)
                .build();
    }

    /**
     * Maps a Role entity to a RoleRequest DTO.
     */
    public static RoleRequest mapToDto(Role role){
        return RoleRequest.builder()
                .roleName(role.getRoleName())
                .id(role.getId())
                .roleType(String.valueOf(role.getRoleType()))
                .build();
    }
}
