package com.middleware.backend.logging.mapper;

import org.mapstruct.Mapper;

import com.middleware.backend.logging.dto.MiddlewareApiCallLogDto;
import com.middleware.backend.logging.model.MiddlewareApiCallLog;

@Mapper(componentModel = "spring")
public interface MiddlewareApiCallLogMapper {

    MiddlewareApiCallLog toEntity(MiddlewareApiCallLogDto dto);

    MiddlewareApiCallLogDto toDto(MiddlewareApiCallLog entity);
}
