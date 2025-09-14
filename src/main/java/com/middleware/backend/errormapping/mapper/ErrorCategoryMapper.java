package com.middleware.backend.errormapping.mapper;

import org.springframework.stereotype.Component;

import com.middleware.backend.errormapping.dto.ErrorCategoryDto;
import com.middleware.backend.errormapping.model.ErrorCategory;

@Component
public class ErrorCategoryMapper {

    public ErrorCategoryDto toDto(ErrorCategory entity) {
        if (entity == null) {
            return null;
        }
        return ErrorCategoryDto.builder().id(entity.getId()).name(entity.getName()).description(entity.getDescription())
                .active(entity.getActive()).createdAt(entity.getCreatedAt()).updatedAt(entity.getUpdatedAt()).build();
    }

    public ErrorCategory toEntity(ErrorCategoryDto dto) {
        if (dto == null) {
            return null;
        }
        return ErrorCategory.builder().id(dto.getId()).name(dto.getName()).description(dto.getDescription())
                .active(dto.getActive()).build();
    }
}