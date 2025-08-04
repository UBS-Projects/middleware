package com.middleware.backend.mapper;

import com.middleware.backend.dto.ErrorCategoryDto;
import com.middleware.backend.model.ErrorCategory;
import org.springframework.stereotype.Component;

@Component
public class ErrorCategoryMapper {

    public ErrorCategoryDto toDto(ErrorCategory entity) {
        if (entity == null) {
            return null;
        }
        return ErrorCategoryDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public ErrorCategory toEntity(ErrorCategoryDto dto) {
        if (dto == null) {
            return null;
        }
        return ErrorCategory.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .build();
    }
}