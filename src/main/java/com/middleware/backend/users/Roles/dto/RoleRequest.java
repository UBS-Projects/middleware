package com.middleware.backend.users.Roles.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

/**
 * Minimal role representation used by role-related APIs and mappers.
 */
@Data
@Builder
@AllArgsConstructor
public class RoleRequest {
    /** role id. */
    private long id;
    /** role unique name. */
    private String roleName;
    /** enum name of role type. */
    private String roleType;
}
