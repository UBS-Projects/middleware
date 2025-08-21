package com.middleware.backend.users.Roles.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PermissionsDTO {
    private Long id;
    private String name;
    private List<RoleRequest> roles;
}
