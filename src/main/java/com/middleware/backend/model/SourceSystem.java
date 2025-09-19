package com.middleware.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity representing an upstream source system that emits data.
 * <p>
 * Enforces a unique name, maintains activation state, and records audit
 * information. Lifecycle hooks manage timestamps and default values.
 */
@Entity
@Table(name = "source_systems", uniqueConstraints = {
        @UniqueConstraint(columnNames = "name") // Ensure source system names are unique
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceSystem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 100, nullable = false, unique = true)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Initializes audit timestamps and defaults prior to first persist.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (active == null) {
            active = true;
        }
    }

    /**
     * Refreshes the update timestamp before each update.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}