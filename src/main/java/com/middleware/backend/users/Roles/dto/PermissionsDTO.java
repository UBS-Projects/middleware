package com.middleware.backend.users.Roles.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Permission representation including the roles that hold it.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PermissionsDTO {
    /** permission id. */
    private Long id;
    /** permission unique name. */
    private String name;
    /** roles having this permission. */
    private List<RoleRequest> roles;
}
