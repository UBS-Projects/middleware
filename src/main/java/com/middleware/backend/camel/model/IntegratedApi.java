package com.middleware.backend.camel.model;

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
                @Index(name = "idx_system", columnList = "integrated_system"),
                @Index(name = "idx_bound_api_code", columnList = "bound_api_code")
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
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "api_url", nullable = false, columnDefinition = "TEXT")
    private String apiUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ApiType type;

    @Column(name = "integrated_system", nullable = false, length = 100)
    private String integratedSystem;

    /**
     * Dynamic Route ID that this metadata API is bound to.
     * When type = METADATA, this field stores the routeId from dynamic_routes table.
     * The metadata API will automatically be used when this route is executed.
     * Example values: "integration-mapping-test", "health-data-api"
     *
     * Required only when type = METADATA
     */
    @Column(name = "bound_api_code", length = 100)
    private String boundApiCode;

    /**
     * Flag to indicate if Organization Unit (ou) should be taken from request
     * When true: ou will be taken from API request parameters
     * When false: ou will be taken from apiUrl configuration
     */
    @Column(name = "use_ou_from_request", nullable = false)
    private Boolean useOuFromRequest = false;

    /**
     * Flag to indicate if Period (pe) should be taken from request
     * When true: pe will be taken from API request parameters
     * When false: pe will be taken from apiUrl configuration
     */
    @Column(name = "use_pe_from_request", nullable = false)
    private Boolean usePeFromRequest = false;

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

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;
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
        if (this.useOuFromRequest == null) {
            this.useOuFromRequest = false;
        }
        if (this.usePeFromRequest == null) {
            this.usePeFromRequest = false;
        }
        validateBoundApiCode();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        validateBoundApiCode();
    }

    private void validateBoundApiCode() {
        if (this.type == ApiType.METADATA &&
                (this.boundApiCode == null || this.boundApiCode.trim().isEmpty())) {
            throw new IllegalArgumentException(
                    "boundApiCode (Route ID) is required when type is METADATA. " +
                            "Please specify which dynamic route this metadata API should be bound to."
            );
        }
    }

    public enum ApiType {
        ANALYTICS,
        METADATA
    }
}