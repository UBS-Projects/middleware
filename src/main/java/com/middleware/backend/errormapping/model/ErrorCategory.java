// ErrorCategory Model
package com.middleware.backend.errormapping.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity representing an error category used to classify errors.
 * <p>
 * Enforces unique names and tracks activation plus audit timestamps/users.
 * Lifecycle hooks initialize timestamps and default activation state.
 */
@Entity
@Table(name = "error_categories", uniqueConstraints = { @UniqueConstraint(columnNames = "name") // Ensure category names
                                                                                                // are unique
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 100, nullable = false, unique = true)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

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