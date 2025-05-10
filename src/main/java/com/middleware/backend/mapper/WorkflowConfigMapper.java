package com.middleware.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import com.middleware.backend.dto.WorkflowConfigDTO;
import com.middleware.backend.model.WorkflowConfig;

@Mapper(componentModel = "spring")
public interface WorkflowConfigMapper {

    WorkflowConfigMapper INSTANCE = Mappers.getMapper(WorkflowConfigMapper.class);

    WorkflowConfigDTO toDTO(WorkflowConfig entity);
    WorkflowConfig toEntity(WorkflowConfigDTO dto);
}
