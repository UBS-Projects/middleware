package com.middleware.backend.kaotocamel.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
/**
 * DTO for DHIS2 Analytics items (metadata)
 */
@Data
@NoArgsConstructor
public class ItemsDto extends HashMap<String, ItemDto> {
    // Extends HashMap to handle dynamic keys from DHIS2 response
}