package com.middleware.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "error_mapping")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_api_id", nullable = false)
    private DestinationApi destinationApi;

    @Column(name = "destination_system_name", length = 100)
    private String destinationSystemName;

    @Column(name = "raw_error_substring", columnDefinition = "TEXT", nullable = false)
    private String rawErrorSubstring;

    @Column(name = "match_type", length = 10, nullable = false)
    private String matchType; // CONTAINS, EQUALS, REGEX

    @Column(name = "mapped_error_code", length = 50, nullable = false)
    private String mappedErrorCode;

    @Column(name = "mapped_message", columnDefinition = "TEXT", nullable = false)
    private String mappedMessage;

    @Column(name = "error_category", length = 50)
    private String errorCategory;

    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    @Column(name = "language", length = 10)
    private String language;

    @Column(name = "active")
    private Boolean active;

   // @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private Long createdBy; //Should be user

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
