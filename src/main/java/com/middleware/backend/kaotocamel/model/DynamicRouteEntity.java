package com.middleware.backend.kaotocamel.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dynamic_routes")
@Data
@NoArgsConstructor
public class DynamicRouteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "routeId", nullable = false,unique = true)
    private String routeId; // The unique route identifier (from YAML)
    @Column(name = "description", nullable = false)
    private String description;

    private int version; // Version number of the route


    @Column(name = "path", nullable = false)
    private String path;

    // @Column(name = "http_method", nullable = false)
    private String httpMethod;

    // @Lob
    // private String yamlContent; // YAML source of the route
     @Column(name = "yaml_content", columnDefinition = "TEXT")
    private String yamlContent;

    private boolean active; // Is this version currently active?

    @Column(name = "default_version", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean defaultVersion = false;

    private LocalDateTime createdAt;

    private String comment; // Admin comment for this version

}