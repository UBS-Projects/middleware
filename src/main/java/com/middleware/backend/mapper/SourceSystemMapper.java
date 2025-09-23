package com.middleware.backend.mapper;

import com.middleware.backend.dto.SourceSystemDto;
import com.middleware.backend.model.SourceSystem;
import org.springframework.stereotype.Component;

/**
 * Mapper component for converting between {@code SourceSystem} entities and
 * {@code SourceSystemDto} objects.
 */
@Component
public class SourceSystemMapper {

    /**
     * Converts an entity to a DTO.
     *
     * @param entity the source entity; may be null
     * @return the mapped DTO or null when input is null
     */
    public SourceSystemDto toDto(SourceSystem entity) {
        if (entity == null) {
            return null;
        }
        return SourceSystemDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .active(entity.getActive())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Converts a DTO to an entity instance.
     *
     * @param dto the source DTO; may be null
     * @return the mapped entity or null when input is null
     */
    public SourceSystem toEntity(SourceSystemDto dto) {
        if (dto == null) {
            return null;
        }
        return SourceSystem.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .active(dto.getActive())
                .createdBy(dto.getCreatedBy())
                .createdAt(dto.getCreatedAt())
                .updatedBy(dto.getUpdatedBy())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
}