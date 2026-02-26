package com.middleware.backend.kaotocamel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing a group of attributes with the same attName
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttributeGroupDto {

    /**
     * The attribute name that groups these attributes together
     * Example: "I.C.U", "الاسعاف والطوارئ"
     */
    private String attName;

    /**
     * The attribute code from DHIS2
     * Example: "ICU", "EMERGENCY"
     */
    private String code;

    /**
     * List of attributes belonging to this attribute group
     */
    @Builder.Default
    private List<AttributeDto> attributes = new ArrayList<>();
}