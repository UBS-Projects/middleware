package com.middleware.backend.logging.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.context.annotation.Primary;

import com.middleware.backend.logging.dto.MiddlewareApiCallLogDto;
import com.middleware.backend.logging.model.MiddlewareApiCallLog;

@Mapper(componentModel = "spring")
@Primary
public interface MiddlewareApiCallLogMapper {

    MiddlewareApiCallLog toEntity(MiddlewareApiCallLogDto dto);

    @Mapping(target = "userId", source = "userId")
    MiddlewareApiCallLogDto toDto(MiddlewareApiCallLog entity);
}