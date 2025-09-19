package com.middleware.backend.users.Roles.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Permission entity representing a fine-grained authority.
 * Uniquely identified by name and linked to roles via many-to-many.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "permissions")
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    /** primary key. */
    private Long id;

    @Column(nullable = false, unique = true)
    /** unique permission name (e.g., "user:view"). */
    private String name;
    @ManyToMany(mappedBy = "permissions", fetch = FetchType.EAGER)
    private List<Role> roles;
}