package com.middleware.backend.integrated_systems.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.sql.Timestamp;

/**
 * Entity representing an Integrated System configuration.
 * <p>
 * Stores system connection details, authentication information, optional metadata,
 * and audit fields for creation and updates.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "integrated_system")
public class IntegratedSystem {

    /** Primary key identifier */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique system code (cannot be null) */
    @Column(name = "code", unique = true, nullable = false)
    private String code;

    /** Host address (IP or domain) of the system */
    private String host;

    /** Port of the system */
    private String port;

    /** Optional human-readable description */
    private String description;

    /**
     * High-level system category (HTTP API, database, broker, email, file transfer).
     * Existing records default to {@link SystemType#HTTP_API}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "system_type")
    @Builder.Default
    private SystemType systemType = SystemType.HTTP_API;

    /** Communication protocol used (HTTP, HTTPS, PostgreSQL, SMTP, …) */
    @Enumerated(EnumType.STRING)
    private Protocol protocol;

    /** Optional additional key/value pair 1 */
    private String additionalKey1;
    private String additionalValue1;

    /** Optional additional key/value pair 2 */
    private String additionalKey2;
    private String additionalValue2;

    /** Authentication type used to access the system */
    @Enumerated(EnumType.STRING)
    private AuthenticationType authenticationType;

    /** Username for BASIC authentication (used only if authenticationType == BASIC) */
    private String username;

    /** Password for BASIC authentication (used only if authenticationType == BASIC) */
    private String password;

    /** JWT token for authentication (used only if authenticationType == JWT) */
    @Column(length = 1000)
    private String token;

    /** User who created this system entry */
    private String createdBy;

    /** Timestamp when this system entry was created */
    private Timestamp createdAt;

    /** User who last updated this system entry */
    private String updatedBy;

    /** Timestamp when this system entry was last updated */
    private Timestamp updatedAt;

    /**
     * Ensures existing HTTP/HTTPS rows (and payloads that omit systemType)
     * persist as {@link SystemType#HTTP_API}.
     */
    @PrePersist
    @PreUpdate
    private void ensureSystemType() {
        if (systemType == null) {
            systemType = SystemType.fromProtocol(protocol);
        }
    }
}
