package com.middleware.backend.camel.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for creating/updating Integration Mappings
 * Now uses dynamicRouteId instead of middlewareApiName
 */
@Data
public class IntegrationMappingRequestDto {
   @NotNull(message = "Dynamic Route ID is required")
   private String dynamicRouteId;

   @NotNull(message = "Integrated API ID is required")
   private Long integratedApiId;

   @NotNull(message = "Mapping type is required")
   private String mappingType;

   @NotNull(message = "Data is required")
   private String data;

   private String attribute;

   @NotNull(message = "External key is required")
   private String externalKey;

   private Boolean isActive;
   private String notes;
}