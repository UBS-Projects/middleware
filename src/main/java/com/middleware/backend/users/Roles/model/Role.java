package com.middleware.backend.users.Roles.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

/**
 * Role entity grouping permissions and route permissions.
 * Includes audit fields and a type to distinguish USER vs SYSTEM_USER.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "role")
@Entity
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    /** primary key. */
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
    /** permissions granted to this role. */
    private List<Permission> permissions;


    @ManyToMany(fetch = FetchType.EAGER)
    @JsonIgnore
    @JoinTable(
            name = "role_routes_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "route_id")
    )
    /** route-level permissions for dynamic endpoints. */
    private List<RoutesPermissions> routesPermissions;

    @Column(nullable = false,name = "created_by")
    /** audit: creator email/id. */
    private String createdBy;

    @Column(nullable = false,name = "created_at")
    /** audit: creation timestamp. */
    private Timestamp createdAt;


    @Column(nullable = false,name = "updated_by")
    /** audit: last updater email/id. */
    private String updatedBy;

    @Column(nullable = false,name = "updated_at")
    /** audit: last update timestamp. */
    private Timestamp updatedAt;

    public enum RoleType{
        USER,
        SYSTEM_USER
    }

}
