package com.middleware.backend.model;

import jakarta.persistence.*;
import lombok.*;
import com.middleware.backend.model.ErrorCategory;
import com.middleware.backend.model.SourceSystem;
import java.time.LocalDateTime;

@Entity
@Table(name = "error_mapping")
 @NamedEntityGraph(
        name = "ErrorMapping.withCategoryAndSourceSystem",
        attributeNodes = {
                @NamedAttributeNode("errorCategory"),
                @NamedAttributeNode("sourceSystem")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_id", length = 255, nullable = false)
    private String routeId; // Reference to DynamicRouteEntity.routeId

    @Column(name = "route_path", length = 500)
    private String routePath; // Path from the route (e.g., /api/users)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_system_id")
    private SourceSystem sourceSystem; // Optional source system reference

    @Column(name = "raw_error_substring", columnDefinition = "TEXT", nullable = false)
    private String rawErrorSubstring;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false)
    private MatchType matchType; // CONTAINS, EQUALS, REGEX

    @Column(name = "mapped_error_code", length = 50, nullable = false)
    private String mappedErrorCode;

    @Column(name = "mapped_message", columnDefinition = "TEXT", nullable = false)
    private String mappedMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "error_category_id")
    private ErrorCategory errorCategory;

    @Column(name = "http_status_code", nullable = false)
    private Integer httpStatusCode;

    @Column(name = "language", length = 10)
    private String language = "en";

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (language == null) {
            language = "en";
        }
        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum MatchType {
        CONTAINS("Substring match"),
        EQUALS("Exact match"),
        REGEX("Regular expression");

        private final String description;

        MatchType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}