package com.middleware.backend.camel.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for IntegratedApi entity
 * Includes boundApiCode for metadata API binding
 */
// IntegratedApiDto.java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegratedApiDto {
    private Long id;
    private String code;
    private String name;
    private String apiUrl;
    private String type;
    private String integratedSystem;
    private String boundApiCode;
    private Boolean useOuFromRequest;
    private Boolean usePeFromRequest;
    private Boolean isActive;
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

     private String createdBy;
    private String updatedBy;
}