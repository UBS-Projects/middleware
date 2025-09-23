package com.middleware.backend.users.Roles.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request payload used to replace the permissions assigned to a role.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PermissionEditDto {
    /** role to update. */
    private String roleName;
    /** permission names to assign to the role. */
    private List<String> permissions;
}
