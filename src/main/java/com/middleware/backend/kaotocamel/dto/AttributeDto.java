package com.middleware.backend.kaotocamel.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
/**
 * Attribute DTO for field values in middleware output
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttributeDto {
    private String name;       // External key
    private Object value;      // Can be number, string, or null
}
