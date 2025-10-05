package com.middleware.backend.integrated_systems.mapper;

import com.middleware.backend.integrated_systems.dto.IntegratedSystemDto;
import com.middleware.backend.integrated_systems.model.IntegratedSystem;

public class IntegratedSystemMapper {

    public static IntegratedSystemDto toDto(IntegratedSystem entity) {
        if (entity == null) return null;
        return IntegratedSystemDto.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .host(entity.getHost())
                .port(entity.getPort())
                .description(entity.getDescription())
                .protocol(entity.getProtocol())
                .additionalAttribute1(entity.getAdditionalAttribute1())
                .additionalAttribute2(entity.getAdditionalAttribute2())
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

    public static IntegratedSystem toEntity(IntegratedSystemDto dto) {
        if (dto == null) return null;
        return IntegratedSystem.builder()
                .id(dto.getId())
                .code(dto.getCode())
                .host(dto.getHost())
                .port(dto.getPort())
                .description(dto.getDescription())
                .protocol(dto.getProtocol())
                .additionalAttribute1(dto.getAdditionalAttribute1())
                .additionalAttribute2(dto.getAdditionalAttribute2())
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