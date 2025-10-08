package com.middleware.backend.system_settings.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO representing DHIS2 settings payload exchanged via the API.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConfigDto {
    private UUID id;
    private String key;
    private String value;
    private String type;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt = LocalDateTime.now();
    private String updatedBy;
    private LocalDateTime updatedAt = LocalDateTime.now();
}

