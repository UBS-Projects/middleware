package com.middleware.backend.kaotocamel.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * Entity representing field mapping configuration for middleware APIs
 * Now linked to Dynamic Routes instead of middleware API names
 */
@Entity
@Table(name = "integration_mapping",
        indexes = {
                @Index(name = "idx_dynamic_route", columnList = "dynamic_route_id"),
                @Index(name = "idx_integrated_api", columnList = "integrated_api_id"),
                @Index(name = "idx_mapping_type", columnList = "mapping_type"),
                @Index(name = "idx_external_key", columnList = "external_key")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_mapping_unique",
                        columnNames = {"dynamic_route_id", "integrated_api_id",
                                "mapping_type", "data", "external_key"}
                )
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Dynamic Route ID that this mapping is bound to
     * References the routeId from DynamicRouteEntity
     */
    @Column(name = "dynamic_route_id", nullable = false, length = 100)
    private String dynamicRouteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "integrated_api_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_mapping_api"))
    @JsonIgnore
    private IntegratedApi integratedApi;

    @Column(name = "integrated_api_id", insertable = false, updatable = false)
    private Long integratedApiId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mapping_type", nullable = false, length = 50)
    private MappingType mappingType;

    @Column(name = "data", columnDefinition = "TEXT", nullable = false)
    private String data;

    @Column(name = "attribute", length = 50)
    private String attribute;

    @Column(name = "external_key", nullable = false, length = 100)
    private String externalKey;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
        if (this.data == null || this.data.trim().isEmpty()) {
            throw new IllegalArgumentException("Data field is required and cannot be empty");
        }
        validateData();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.data == null || this.data.trim().isEmpty()) {
            throw new IllegalArgumentException("Data field is required and cannot be empty");
        }
        validateData();
    }

    private void validateData() {
        if (data == null || data.trim().isEmpty()) {
            throw new IllegalArgumentException("Data field is required and cannot be empty");
        }

        if (mappingType != null) {
            switch (mappingType) {
                case DATA_ELEMENT_WITH_DISAGGREGATION:
                    if (!data.contains(".")) {
                        throw new IllegalArgumentException(
                                "DATA_ELEMENT_WITH_DISAGGREGATION requires data in format DE_UID.COC_UID. Current value: '" + data + "'"
                        );
                    }
                    String[] parts = data.split("\\.");
                    if (parts.length != 2) {
                        throw new IllegalArgumentException(
                                "DATA_ELEMENT_WITH_DISAGGREGATION must have exactly 2 parts. Format: DE_UID.COC_UID. Current value: '" + data + "'"
                        );
                    }
                    break;

                case DATA_ELEMENT:
                    if (data.contains(".")) {
                        throw new IllegalArgumentException(
                                "DATA_ELEMENT should not contain disaggregation (no dots). Current value: '" + data + "'"
                        );
                    }
                    break;

                case INDICATOR:
                    if (data.length() < 3) {
                        throw new IllegalArgumentException(
                                "Indicator UID must be at least 3 characters. Current value: '" + data + "'"
                        );
                    }
                    break;

                default:
                    break;
            }
        }
    }

    public enum MappingType {
        DATA_ELEMENT,
        DATA_ELEMENT_WITH_DISAGGREGATION,
        INDICATOR
    }
}