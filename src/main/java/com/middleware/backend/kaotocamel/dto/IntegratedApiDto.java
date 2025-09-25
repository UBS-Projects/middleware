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
 * DTO for IntegratedApi entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegratedApiDto {
    private Long id;
    private String code;
    private String name;
    private String apiUrl;
    private String type;  // String representation of ApiType enum
    private String integratedSystem;
    private Boolean isActive;
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}