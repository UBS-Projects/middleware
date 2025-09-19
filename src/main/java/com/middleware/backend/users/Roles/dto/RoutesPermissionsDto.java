package com.middleware.backend.users.Roles.dto;

import com.middleware.backend.users.Roles.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Representation of a route permission record with its assigned roles.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoutesPermissionsDto {
    /** database identifier. */
    private Long id;
    /** unique route identifier. */
    private String routeId;
    /** roles that have access to this route. */
    private List<RoleRequest> roles;
}
