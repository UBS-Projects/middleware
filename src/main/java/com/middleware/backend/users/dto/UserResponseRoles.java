package com.middleware.backend.users.dto;

import com.middleware.backend.users.model.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Minimal user projection used when listing users with roles context.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponseRoles {
    /** user id. */
    private Long id;
    /** display name/username. */
    private String userName;
    /** user email. */
    private String email;
    /** current status. */
    private Status status;
}
