package com.middleware.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data transfer object for source system records.
 * <p>
 * Used to move data between the API and service layers without exposing the
 * JPA entity, including audit fields and activation flag.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceSystemDto {
    private Long id;
    private String name;
    private String description;
    private Boolean active;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}