package com.middleware.backend.users.Roles.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "role")
@Entity
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true,nullable = false, name = "role_name")
    private String roleName;
    @Column(name = "role_type")
    private RoleType roleType;
    @ManyToMany(fetch = FetchType.EAGER)
    @JsonIgnore
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private List<Permission> permissions;


    @ManyToMany(fetch = FetchType.EAGER)
    @JsonIgnore
    @JoinTable(
            name = "role_routes_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "route_id")
    )
    private List<RoutesPermissions> routesPermissions;

    @Column(nullable = false,name = "created_by")
    private String createdBy;

    @Column(nullable = false,name = "created_at")
    private Timestamp createdAt;


    @Column(nullable = false,name = "updated_by")
    private String updatedBy;

    @Column(nullable = false,name = "updated_at")
    private Timestamp updatedAt;

    public enum RoleType{
        USER,
        SYSTEM_USER
    }

}
