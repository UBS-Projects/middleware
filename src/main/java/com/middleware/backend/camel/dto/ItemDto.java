package com.middleware.backend.camel.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * DTO for individual metadata item
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemDto {
    private String uid;
    private String code;
    private String name;
    private String dimensionItemType;
}