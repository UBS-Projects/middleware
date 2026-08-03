package com.middleware.backend.camel.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import java.util.ArrayList;
/**
 * Response DTO for middleware API output
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiddlewareResponseDto {
    @Builder.Default
    private List<MiddlewareRowDto> rows = new ArrayList<>();
}
