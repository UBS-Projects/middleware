package com.middleware.backend.kaotocamel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO to hold period data with its attributes
 * Used to group periods inside an organization unit
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodDataDto {

    /**
     * Period name (e.g., "May 2025", "July 2025")
     */
    private String period;

    /**
     * List of attributes/data for this period
     */
    private List<AttributeDto> attributes;
}