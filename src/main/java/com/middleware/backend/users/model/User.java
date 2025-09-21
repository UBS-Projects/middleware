package com.middleware.backend.users.model;

import com.middleware.backend.users.Roles.model.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

/**
 * Entity representing an application user.
 * <p>
 * Maps to the {@code users} table in the database and stores
 * authentication, authorization, and auditing information.
 * </p>
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {

    /** Primary key identifier for the user */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique username chosen by the user */
    @Column(nullable = false, name = "user_name")
    private String userName;

    /** Unique email address of the user */
    @Column(unique = true, nullable = false, name = "email")
    private String email;

    /** Status of the user (e.g., ACTIVE, INACTIVE) */
    @Enumerated(EnumType.STRING)
    private Status status;

    /** The username or identifier of the creator of this record */
    @Column(nullable = false, name = "created_by")
    private String createdBy;

    /** Timestamp when the user record was created */
    @Column(nullable = false, name = "created_at")
    private Timestamp createdAt;

    /** The username or identifier of the last person who updated this record */
    @Column(nullable = false, name = "updated_by")
    private String updatedBy;

    /** Timestamp when the user record was last updated */
    @Column(nullable = false, name = "updated_at")
    private Timestamp updatedAt;

    /** Hashed password for the user */
    @Column(nullable = false, name = "password")
    private String password;

    /** Roles assigned to the user for authorization purposes */
    @ManyToMany(fetch = FetchType.EAGER)
    private List<Role> roles;
}
