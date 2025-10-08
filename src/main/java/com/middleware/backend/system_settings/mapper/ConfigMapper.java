package com.middleware.backend.system_settings.mapper;

import com.middleware.backend.system_settings.dto.ConfigDto;
import com.middleware.backend.system_settings.model.Config;
import com.middleware.backend.system_settings.model.ConfigType;

/**
 * Mapping utilities between DHIS2 entity and DTO.
 */
public class ConfigMapper {

    /**
     * Converts entity to DTO for responses.
     */
    public static ConfigDto toDTO(Config config) {
        return ConfigDto.builder()
                .id(config.getId())
                .key(config.getKey())
                .value(config.getValue())
                .type(config.getType().name())
                .description(config.getDescription())
                .createdAt(config.getCreatedAt())
                .createdBy(config.getCreatedBy())
                .updatedAt(config.getUpdatedAt())
                .updatedBy(config.getUpdatedBy())
                .build();
    }

    /**
     * Converts DTO to entity for persistence.
     */
    public static Config toEntity(ConfigDto dto) {
        return Config.builder()
                .id(dto.getId())
                .key(dto.getKey())
                .value(dto.getValue())
                .type(dto.getType() != null ? ConfigType.valueOf(dto.getType()) : ConfigType.STRING)
                .description(dto.getDescription())
                .createdAt(dto.getCreatedAt())
                .createdBy(dto.getCreatedBy())
                .updatedAt(dto.getUpdatedAt())
                .updatedBy(dto.getUpdatedBy())
                .build();
    }
}
