package com.middleware.backend.mapper;

import com.middleware.backend.dto.SourceSystemDto;
import com.middleware.backend.model.SourceSystem;
import org.springframework.stereotype.Component;

@Component
public class SourceSystemMapper {

    public SourceSystemDto toDto(SourceSystem entity) {
        if (entity == null) {
            return null;
        }
        return SourceSystemDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public SourceSystem toEntity(SourceSystemDto dto) {
        if (dto == null) {
            return null;
        }
        return SourceSystem.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .active(dto.getActive())
                .build();
    }
}