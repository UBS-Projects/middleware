package com.middleware.backend.camel.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;

/**
 * DTO for DHIS2 Analytics items (metadata)
 */
@Data
@NoArgsConstructor
public class ItemsDto extends HashMap<String, ItemDto> {
    // Extends HashMap to handle dynamic keys from DHIS2 response
}