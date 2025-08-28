package com.middleware.backend.users.Roles.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoutesPermissionsRequest {
    private Long id;
    private String routeId;
}
