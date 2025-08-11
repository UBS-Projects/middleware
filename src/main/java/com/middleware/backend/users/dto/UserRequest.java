package com.middleware.backend.users.dto;


import com.middleware.backend.users.Roles.dto.RoleRequest;
import com.middleware.backend.users.Roles.model.Role;
import com.middleware.backend.users.model.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class UserRequest {
    private String userName;
    private String email;
    private Status status;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String password;
    private List<RoleRequest> roles;

}
