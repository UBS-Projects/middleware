package com.middleware.backend.kaotocamel.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for IntegratedApi entity
 * Includes boundApiCode for metadata API binding
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegratedApiDto {

    private Long id;

    private String code;

    private String name;

    private String apiUrl;

    private String type; // ANALYTICS, METADATA

    private String integratedSystem;

    @JsonProperty("boundApiCode")
    private String boundApiCode;

    private Boolean isActive;

    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}