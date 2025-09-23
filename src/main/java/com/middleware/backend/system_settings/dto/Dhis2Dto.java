package com.middleware.backend.system_settings.dto;

import com.middleware.backend.system_settings.model.Dhis2;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing DHIS2 settings payload exchanged via the API.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Dhis2Dto {
    private String baseUrl;
    private String userName;
    private String password;
    private Long timeout;
    private Long connectTimeout;
}
