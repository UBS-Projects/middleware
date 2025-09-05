package com.middleware.backend.users.Roles.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class RoleRequest {
    private long id;
    private String roleName;
    private String roleType;
}
