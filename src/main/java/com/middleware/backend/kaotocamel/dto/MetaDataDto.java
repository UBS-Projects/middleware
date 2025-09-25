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
 * DTO for DHIS2 Analytics metadata
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetaDataDto {
    private DimensionsDto dimensions;
    private ItemsDto items;
}