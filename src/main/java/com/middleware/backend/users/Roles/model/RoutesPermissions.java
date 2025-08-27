package com.middleware.backend.users.Roles.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "routes_permissions")
public class RoutesPermissions {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String routeId;
    @ManyToMany(mappedBy = "routesPermissions", fetch = FetchType.EAGER)
    private List<Role> roles;
}

