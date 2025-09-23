package com.middleware.backend.logging.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.context.annotation.Primary;

import com.middleware.backend.logging.dto.MiddlewareApiCallLogDto;
import com.middleware.backend.logging.model.MiddlewareApiCallLog;

@Mapper(componentModel = "spring")
@Primary
/**
 * MapStruct mapper to convert between {@link MiddlewareApiCallLog} entity and {@link MiddlewareApiCallLogDto}.
 */
public interface MiddlewareApiCallLogMapper {
    /** Converts a DTO into a new entity instance. */
    MiddlewareApiCallLog toEntity(MiddlewareApiCallLogDto dto);
    /** Converts an entity into its DTO representation. */
    @Mapping(target = "userId", source = "userId")
    MiddlewareApiCallLogDto toDto(MiddlewareApiCallLog entity);
}