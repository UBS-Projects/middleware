package com.middleware.backend.kaotocamel.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;/**
 * Request DTO for creating/updating IntegrationMapping
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrationMappingRequestDto {
    private String middlewareApiName;
    private Long integratedApiId;
    private String mappingType;
    private String data;
    private String attribute;
    private String externalKey;
    private Boolean isActive;
    private String notes;
}
