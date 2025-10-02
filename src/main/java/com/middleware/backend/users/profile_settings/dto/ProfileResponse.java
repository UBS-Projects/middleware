package com.middleware.backend.users.profile_settings.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProfileResponse {
    /** Unique username chosen by the user */
    private String userName;
    /** Unique email address of the user */
    private String email;
    /** The username or identifier of the creator of this record */
    private String createdBy;
    /** Timestamp when the user record was created */
    private Timestamp createdAt;
    /** The username or identifier of the last person who updated this record */
    private String updatedBy;
    /** Timestamp when the user record was last updated */
    private Timestamp updatedAt;
    /** Roles assigned to the user for authorization purposes */
    private String roles;
}
