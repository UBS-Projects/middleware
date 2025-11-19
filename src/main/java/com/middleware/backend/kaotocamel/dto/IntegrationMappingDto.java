package com.middleware.backend.kaotocamel.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * DTO for Integration Mapping entity
 * Now includes dynamicRouteId instead of middlewareApiName
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrationMappingDto {
    private Long id;
    private String dynamicRouteId;
    private Long integratedApiId;
    private String integratedApiCode;
    private String integratedApiName;
    private String mappingType;
    private String data;
    private String attribute;
    private String externalKey;
    private Boolean isActive;
    private String notes;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}