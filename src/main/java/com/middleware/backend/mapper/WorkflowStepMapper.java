package com.middleware.backend.mapper;

import com.middleware.backend.dto.WorkflowStepDTO;
import com.middleware.backend.model.DestinationApi;
import com.middleware.backend.model.WorkflowConfig;
import com.middleware.backend.model.WorkflowStep;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface WorkflowStepMapper {

    @Mappings({
        @Mapping(source = "id", target = "id"),
        @Mapping(source = "workflowConfig.id", target = "workflowConfigId"),
        @Mapping(source = "parentStep.id", target = "parentStepId"),
        @Mapping(source = "stepOrder", target = "stepOrder"),
        @Mapping(source = "stepName", target = "stepName"),
        @Mapping(source = "stepType", target = "stepType"),
        @Mapping(source = "destinationApi.id", target = "destinationApiId"),
        @Mapping(source = "transformationExpression", target = "transformationExpression"),
        @Mapping(source = "delaySeconds", target = "delaySeconds"),
        @Mapping(source = "conditionExpression", target = "conditionExpression"),
        @Mapping(source = "retryCount", target = "retryCount"),
        @Mapping(source = "retryDelaySeconds", target = "retryDelaySeconds"),
        @Mapping(source = "forkGroupId", target = "forkGroupId"),
        @Mapping(source = "createdBy", target = "createdBy"),
        @Mapping(source = "updatedBy", target = "updatedBy"),
        @Mapping(source = "createdAt", target = "createdAt"),
        @Mapping(source = "updatedAt", target = "updatedAt")
    })
    WorkflowStepDTO toDTO(WorkflowStep entity);

    // --- Reverse mapping ---
    @Mappings({
        @Mapping(target = "workflowConfig", ignore = true),
        @Mapping(target = "parentStep", ignore = true),
        @Mapping(target = "destinationApi", ignore = true)
    })
    WorkflowStep toEntity(WorkflowStepDTO dto);

    // After mapping to manually build objects based on IDs
    @AfterMapping
    default void fillRelations(WorkflowStepDTO dto, @MappingTarget WorkflowStep entity) {
        if (dto.getWorkflowConfigId() != null) {
            WorkflowConfig config = new WorkflowConfig();
            config.setId(dto.getWorkflowConfigId());
            entity.setWorkflowConfig(config);
        }

        if (dto.getParentStepId() != null) {
            WorkflowStep parent = new WorkflowStep();
            parent.setId(dto.getParentStepId());
            entity.setParentStep(parent);
        }

        if (dto.getDestinationApiId() != null) {
            DestinationApi api = new DestinationApi();
            api.setId(dto.getDestinationApiId());
            entity.setDestinationApi(api);
        }
    }
}
