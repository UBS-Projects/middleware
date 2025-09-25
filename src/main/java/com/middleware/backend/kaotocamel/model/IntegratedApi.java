package com.middleware.backend.kaotocamel.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity representing an integrated API definition from DHIS2
 * This stores reusable API endpoint definitions
 */
@Entity
@Table(name = "integrated_apis",
        indexes = {
                @Index(name = "idx_api_type", columnList = "type"),
                @Index(name = "idx_api_code", columnList = "code"),
                @Index(name = "idx_system", columnList = "integrated_system")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_api_code", columnNames = {"code"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntegratedApi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;  // Technical code (unique identifier)

    @Column(name = "name", nullable = false, length = 255)
    private String name;  // Descriptive name

    @Column(name = "api_url", nullable = false, columnDefinition = "TEXT")
    private String apiUrl;  // Relative URI (may include filters like pe:...)

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ApiType type;  // ANALYTICS, DATAVALUE, METADATA

    @Column(name = "integrated_system", nullable = false, length = 100)
    private String integratedSystem;  // e.g., HMIS-DWH, DHIS2-Play

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "integratedApi", fetch = FetchType.LAZY)
    private List<IntegrationMapping> mappings;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Enum for API types
     */
    public enum ApiType {
        ANALYTICS,
        DATAVALUE,
        METADATA
    }
}