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
 * Defines how to extract data from DHIS2 responses and map to output fields
 */
@Entity
@Table(name = "integration_mapping",
        indexes = {
                @Index(name = "idx_middleware_api", columnList = "middleware_api_name"),
                @Index(name = "idx_integrated_api", columnList = "integrated_api_id"),
                @Index(name = "idx_mapping_type", columnList = "mapping_type"),
                @Index(name = "idx_external_key", columnList = "external_key")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_mapping_unique",
                        columnNames = {"middleware_api_name", "integrated_api_id",
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

    @Column(name = "middleware_api_name", nullable = false, length = 100)
    private String middlewareApiName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "integrated_api_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_mapping_api"))
    @JsonIgnore
    private IntegratedApi integratedApi;

    @Column(name = "integrated_api_id", insertable = false, updatable = false)
    private Long integratedApiId;  // For easier querying

    @Enumerated(EnumType.STRING)
    @Column(name = "mapping_type", nullable = false, length = 50)
    private MappingType mappingType;

    @Column(name = "data", columnDefinition = "TEXT")
    private String data;  // Content depends on mappingType
    /*
     * DATA_ELEMENT => DE_UID
     * DATA_ELEMENT_WITH_DISAGGREGATION => DE_UID.COC_UID
     * INDICATOR => INDICATOR_UID
     * META_OU_NAME => "ou.name"
     */

    @Column(name = "attribute", length = 50)
    private String attribute;  // Optional: AOC/Attribute for future use

    @Column(name = "external_key", nullable = false, length = 100)
    private String externalKey;  // Final field name in output

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
        validateData();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        validateData();
    }

    /**
     * Validates data field based on mapping type
     */
    private void validateData() {
        if (mappingType != null && data != null) {
            switch (mappingType) {
                case DATA_ELEMENT_WITH_DISAGGREGATION:
                    if (!data.contains(".")) {
                        throw new IllegalArgumentException(
                                "DATA_ELEMENT_WITH_DISAGGREGATION requires data in format DE_UID.COC_UID"
                        );
                    }
                    break;
                case DATA_ELEMENT:
                    if (data.contains(".")) {
                        throw new IllegalArgumentException(
                                "DATA_ELEMENT should not contain disaggregation (no dots)"
                        );
                    }
                    break;
                case DATA_ELEMENT_WITH_DISAGGREGATION_AND_ATTRIBUTE:
                    // ✅ NEW VALIDATION
                    if (!data.contains(".") || data.split("\\.").length < 2) {
                        throw new IllegalArgumentException(
                                "DATA_ELEMENT_WITH_DISAGGREGATION_AND_ATTRIBUTE requires data in format DE_UID.COC_UID.AOC_UID or DE_UID.AOC_UID"
                        );
                    }
                    break;
                case INDICATOR:
                    // Indicator UIDs are simple strings
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Enum for mapping types
     */
    public enum MappingType {
        DATA_ELEMENT,
        DATA_ELEMENT_WITH_DISAGGREGATION,
        DATA_ELEMENT_WITH_DISAGGREGATION_AND_ATTRIBUTE,
        INDICATOR
    }
}