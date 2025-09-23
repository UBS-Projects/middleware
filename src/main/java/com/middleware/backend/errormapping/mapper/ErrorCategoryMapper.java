package com.middleware.backend.errormapping.mapper;

import com.middleware.backend.dto.ErrorCategoryDto;
import com.middleware.backend.errormapping.model.ErrorCategory;
import org.springframework.stereotype.Component;

/**
 * Mapper component for converting between {@code ErrorCategory} entities and
 * {@code ErrorCategoryDto} objects.
 */
@Component
public class ErrorCategoryMapper {

    /**
     * Converts an entity to a DTO.
     *
     * @param entity the source entity; may be null
     * @return the mapped DTO or null when input is null
     */
    public ErrorCategoryDto toDto(ErrorCategory entity) {
        if (entity == null) {
            return null;
        }
        return ErrorCategoryDto.builder()
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
    public ErrorCategory toEntity(ErrorCategoryDto dto) {
        if (dto == null) {
            return null;
        }
        return ErrorCategory.builder()
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