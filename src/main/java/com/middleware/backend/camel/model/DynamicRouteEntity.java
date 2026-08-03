package com.middleware.backend.camel.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a versioned, database-backed definition of a dynamic Camel route.
 * Each persisted record corresponds to a specific version of a route identified by {@code routeId}.
 * The route definition is stored as YAML in {@code yamlContent}, in addition to metadata
 * such as HTTP method, path, activation status, and audit fields.
 */
@Entity
@Table(name = "dynamic_routes")
@Data
@NoArgsConstructor
public class DynamicRouteEntity {
    /** Unique identifier for this route version entry. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stable route key across versions (typically the YAML route id). */
    @Column(name = "routeId", nullable = false)
    private String routeId;

    /** Human-readable description of the route's purpose. */
    @Column(name = "description", nullable = false)
    private String description;

    /** Monotonic version number for the route definition. */
    private int version;

    /** URL path that this route handles. */
    @Column(name = "path", nullable = false)
    private String path;

    /** HTTP method (e.g., GET, POST) that this route expects. */
    private String httpMethod;

    /** YAML source that defines the Camel route. */
    @Column(name = "yaml_content", columnDefinition = "TEXT")
    private String yamlContent;

    /** Whether this version is currently active and deployed. */
    private boolean active;

    /** Whether this is the default version to serve when multiple versions exist. */
    @Column(name = "default_version", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean defaultVersion = false;

    /** Username or identifier for the creator of this route version. */
    @Column(name = "created_by", nullable = false)
    private String createdBy;

    /** Timestamp when this route version was created. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Username or identifier for the last updater of this route version. */
    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    /** Timestamp when this route version was last updated. */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Optional administrative comment describing this version or change rationale. */
    private String comment;
}