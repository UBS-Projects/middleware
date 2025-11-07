package com.middleware.backend.kaotocamel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO to hold period data with its attribute groups
 * Attributes are now grouped by attName
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
     * List of attribute groups organized by attName
     */
    @Builder.Default
    private List<AttributeGroupDto> attributeGroups = new ArrayList<>();
}