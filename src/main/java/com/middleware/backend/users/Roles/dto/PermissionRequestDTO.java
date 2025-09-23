package com.middleware.backend.users.Roles.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload to create or assign a permission to a role.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PermissionRequestDTO {
    /** permission name. */
    private String name;
    /** target role name. */
    private String roleName;
}
