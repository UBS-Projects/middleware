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
