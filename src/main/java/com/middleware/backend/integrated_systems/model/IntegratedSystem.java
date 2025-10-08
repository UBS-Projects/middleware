package com.middleware.backend.integrated_systems.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "integrated_system")
public class IntegratedSystem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code",unique = true,nullable = false)
    private String code;

    private String host;

    private String port;

    private String description;


    @Enumerated(EnumType.STRING)
    private Protocol protocol;

    private String additionalKey1;
    private String additionalValue1;

    private String additionalKey2;
    private String additionalValue2;


    @Enumerated(EnumType.STRING)
    private AuthenticationType authenticationType;


    // Only used if authenticationType == BASIC
    private String username;
    private String password;

    // Only used if authenticationType == JWT
    private String token;

    private String createdBy;
    private Timestamp createdAt;
    private String updatedBy;
    private Timestamp updatedAt;

}
