package com.middleware.backend.users.dto;

import com.middleware.backend.users.Roles.dto.RoleRequest;
import com.middleware.backend.users.Roles.model.Role;
import com.middleware.backend.users.model.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

/**
 * DTO returned by user APIs.
 */
@Data
@AllArgsConstructor
@Builder
public class UserResponse {
    /** user id. */
    private Long id;
    /** display name/username. */
    private String userName;
    /** user email (login identifier). */
    private String email;
    /** current user status. */
    private Status status;
    /** audit: creator identifier. */
    private String createdBy;
    /** audit: creation timestamp. */
    private Timestamp createdAt;
    /** audit: last updater identifier. */
    private String updatedBy;
    /** audit: last update timestamp. */
    private Timestamp updatedAt;
    /** user's password (usually omitted in responses). */
    private String password;
    /** roles assigned to the user. */
    private List<RoleRequest> roles;

}
