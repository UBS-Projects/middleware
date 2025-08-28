package com.middleware.backend.users.Roles.dto;

import com.middleware.backend.users.Roles.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoutesPermissionsDto {
    private Long id;
    private String routeId;
    private List<RoleRequest> roles;
}
