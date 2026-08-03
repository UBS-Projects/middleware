package com.middleware.backend.camel.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * DTO for DHIS2 Analytics metadata
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetaDataDto {
    private DimensionsDto dimensions;
    private ItemsDto items;
}