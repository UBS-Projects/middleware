package com.middleware.backend.kaotocamel.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * Row DTO for middleware API output
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiddlewareRowDto {
    private String ou;         // Organization unit UID
    private String ouName;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)

    // NEW: Full orgUnit metadata// Organization unit name
    private Map<String, Object> ouDetails;
    private String period;     // Period name/code

    @Builder.Default
    private List<AttributeDto> attributes = new ArrayList<>();
}