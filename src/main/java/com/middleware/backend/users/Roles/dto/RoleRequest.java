package com.middleware.backend.users.Roles.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Builder
@AllArgsConstructor
public class RoleRequest {
    private long id;
    private String roleName;
    private String roleType;
}
