package com.middleware.backend.users.Roles.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Route permission entity representing access to a dynamic route.
 * Each record links a route identifier to a set of roles.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "routes_permissions")
public class RoutesPermissions {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    /** primary key. */
    private Long id;
    /** unique route identifier used in authorization checks. */
    private String routeId;
    @ManyToMany(mappedBy = "routesPermissions", fetch = FetchType.EAGER)
    private List<Role> roles;
}

