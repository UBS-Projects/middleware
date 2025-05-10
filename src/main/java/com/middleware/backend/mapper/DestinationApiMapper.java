package com.middleware.backend.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import com.middleware.backend.dto.DestinationApiDTO;
import com.middleware.backend.model.DestinationApi;

@Mapper(componentModel = "spring")
public interface DestinationApiMapper {

    DestinationApiMapper INSTANCE = Mappers.getMapper(DestinationApiMapper.class);

    DestinationApiDTO toDTO(DestinationApi entity);
    DestinationApi toEntity(DestinationApiDTO dto);
}
