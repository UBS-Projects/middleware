package com.middleware.backend.system_settings.mapper;

import com.middleware.backend.system_settings.dto.Dhis2Dto;
import com.middleware.backend.system_settings.model.Dhis2;

/**
 * Mapping utilities between DHIS2 entity and DTO.
 */
public class Dhis2Mapper {

    /**
     * Converts entity to DTO for responses.
     */
    public static Dhis2Dto mapToDto(Dhis2 entity) {
        return Dhis2Dto.builder()
                .baseUrl(entity.getBaseUrl())
                .userName(entity.getUserName())
                .password(entity.getPassword())
                .timeout(entity.getTimeout())
                .connectTimeout(entity.getConnectTimeout())
                .build();
    }

    /**
     * Converts DTO to entity for persistence.
     */
    public static Dhis2 mapToEntity(Dhis2Dto dto) {
        return Dhis2.builder()
                .baseUrl(dto.getBaseUrl())
                .userName(dto.getUserName())
                .password(dto.getPassword())
                .timeout(dto.getTimeout())
                .connectTimeout(dto.getConnectTimeout())
                .build();
    }
}
