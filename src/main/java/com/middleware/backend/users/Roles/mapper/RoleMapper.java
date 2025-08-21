package com.middleware.backend.users.Roles.mapper;

import com.middleware.backend.users.Roles.dto.RoleRequest;
import com.middleware.backend.users.Roles.model.Role;

public class RoleMapper {
    public static Role mapToEntity(RoleRequest role){
        return Role.builder()
                .roleName(role.getRoleName())
                .id(role.getId()).
                build();
    }

    public static RoleRequest mapToDto(Role role){
        return RoleRequest.builder()
                .roleName(role.getRoleName())
                .id(role.getId()).
                build();
    }
}
