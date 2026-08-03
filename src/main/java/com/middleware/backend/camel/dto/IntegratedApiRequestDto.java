package com.middleware.backend.camel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Request DTO for creating/updating IntegratedApi
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntegratedApiRequestDto {

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "API URL is required")
    private String apiUrl;

    @NotBlank(message = "Type is required")
    private String type;

    @NotBlank(message = "Integrated System is required")
    private String integratedSystem;

    private String boundApiCode;

    private Boolean useOuFromRequest;

    private Boolean usePeFromRequest;

    private Boolean isActive;

    private String description;
}