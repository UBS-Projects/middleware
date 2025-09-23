package com.middleware.backend.users.Roles.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to associate or update a route permission entry.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoutesPermissionsRequest {
    /** database identifier (for updates). */
    private Long id;
    /** unique route identifier used for permission checks. */
    private String routeId;
}
