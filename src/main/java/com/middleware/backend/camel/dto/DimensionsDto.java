package com.middleware.backend.camel.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * DTO for DHIS2 Analytics dimensions
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DimensionsDto {
    private List<String> dx;   // Data dimensions
    private List<String> ou;   // Organization units
    private List<String> pe;   // Periods
}