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
 * Incoming payload used to create or update a user.
 */
@Data
@Builder
@AllArgsConstructor
public class UserRequest {
    /** display name/username. */
    private String userName;
    /** unique email used for login. */
    private String email;
    /** current status e.g., ACTIVE/INACTIVE. */
    private Status status;
    /** audit: creator identifier. */
    private String createdBy;
    /** audit: creation timestamp. */
    private Timestamp createdAt;
    /** audit: last updater identifier. */
    private String updatedBy;
    /** audit: last update timestamp. */
    private Timestamp updatedAt;
    /** raw or hashed password depending on usage context. */
    private String password;
    /** roles to assign to the user. */
    private List<RoleRequest> roles;

}
