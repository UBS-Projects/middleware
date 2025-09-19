package com.middleware.backend.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data transfer object representing an error category.
 * <p>
 * Mirrors the {@code ErrorCategory} entity for API payloads and responses,
 * including audit fields and activation state.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorCategoryDto {
    private Long id;
    private String name;
    private String description;
    private Boolean active;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
