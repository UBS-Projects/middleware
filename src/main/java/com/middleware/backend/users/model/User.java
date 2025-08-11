package com.middleware.backend.users.model;

import com.middleware.backend.users.Roles.model.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,name = "user_name")
    private String userName;

    @Column(nullable = false,name = "email")
    private String email;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(nullable = false,name = "created_at")
    private Timestamp createdAt;

    @Column(nullable = false,name = "updated_at")
    private Timestamp updatedAt;

    @Column(nullable = false,name = "password")
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    private List<Role> roles;
}
