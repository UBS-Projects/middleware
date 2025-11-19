package com.middleware.backend.integrated_systems.mapper;

import com.middleware.backend.integrated_systems.dto.IntegratedSystemDto;
import com.middleware.backend.integrated_systems.model.IntegratedSystem;

/**
 * Mapper class responsible for converting between {@link IntegratedSystem} entities
 * and {@link IntegratedSystemDto} objects.
 */
public class IntegratedSystemMapper {

    /**
     * Converts an {@link IntegratedSystem} entity to a {@link IntegratedSystemDto}.
     *
     * @param entity the {@link IntegratedSystem} entity to convert
     * @return the corresponding {@link IntegratedSystemDto}, or null if the entity is null
     */
    public static IntegratedSystemDto toDto(IntegratedSystem entity) {
        return IntegratedSystemDto.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .host(entity.getHost())
                .port(entity.getPort())
                .description(entity.getDescription())
                .protocol(entity.getProtocol())
                .additionalKey1(entity.getAdditionalKey1())
                .additionalValue1(entity.getAdditionalValue1())
                .additionalKey2(entity.getAdditionalKey2())
                .additionalValue2(entity.getAdditionalValue2())
                .authenticationType(entity.getAuthenticationType())
                .username(entity.getUsername())
                .password(entity.getPassword())
                .token(entity.getToken())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Converts an {@link IntegratedSystemDto} to an {@link IntegratedSystem} entity.
     *
     * @param dto the {@link IntegratedSystemDto} to convert
     * @return the corresponding {@link IntegratedSystem} entity, or null if the DTO is null
     */
    public static IntegratedSystem toEntity(IntegratedSystemDto dto) {
        return IntegratedSystem.builder()
                .id(dto.getId())
                .code(dto.getCode())
                .host(dto.getHost())
                .port(dto.getPort())
                .description(dto.getDescription())
                .protocol(dto.getProtocol())
                .additionalKey1(dto.getAdditionalKey1())
                .additionalValue1(dto.getAdditionalValue1())
                .additionalKey2(dto.getAdditionalKey2())
                .additionalValue2(dto.getAdditionalValue2())
                .authenticationType(dto.getAuthenticationType())
                .username(dto.getUsername())
                .password(dto.getPassword())
                .token(dto.getToken())
                .createdBy(dto.getCreatedBy())
                .createdAt(dto.getCreatedAt())
                .updatedBy(dto.getUpdatedBy())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
}
