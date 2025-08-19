package com.middleware.backend.users.dto;

import com.middleware.backend.users.model.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class UserResponseRoles {
    private Long id;
    private String userName;
    private String email;
    private Status status;
}
