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