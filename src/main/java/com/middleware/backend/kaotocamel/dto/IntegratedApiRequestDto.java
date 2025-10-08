package com.middleware.backend.kaotocamel.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;/**
 * Request DTO for creating/updating IntegratedApi
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegratedApiRequestDto {
    private String code;
    private String name;
    private String apiUrl;
    private String type;
    private String integratedSystem;
    private Boolean isActive;
    private String description;
    @Size(max = 100)
    private String boundApiCode;
}