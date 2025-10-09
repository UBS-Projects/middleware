package com.middleware.backend.notification.mapper;

import com.middleware.backend.notification.dto.NotificationGroupDto;
import com.middleware.backend.notification.model.NotificationGroup;

import java.util.stream.Collectors;

/**
 * Mapper class for converting between {@link NotificationGroup} entities and {@link NotificationGroupDto} DTOs.
 *
 * <p>This class provides static methods to map data between the entity and DTO layers,
 * ensuring consistent representation of notification group data across the application.</p>
 */
public class GroupMapper {

    /**
     * Converts a {@link NotificationGroupDto} to a {@link NotificationGroup} entity.
     *
     * @param group the DTO to convert
     * @return a {@link NotificationGroup} entity with fields copied from the DTO
     */
    public static NotificationGroup mapToEntity(NotificationGroupDto group){
        return NotificationGroup.builder()
                .id(group.getId())
                .name(group.getName())
                .code(group.getCode())
                .description(group.getDescription())
                .active(group.isActive())
                .createdBy(group.getCreatedBy())
                .createdAt(group.getCreatedAt())
                .updatedBy(group.getUpdatedBy())
                .updatedAt(group.getUpdatedAt())
                .build();
    }

    /**
     * Converts a {@link NotificationGroup} entity to a {@link NotificationGroupDto}.
     *
     * @param group the entity to convert
     * @return a {@link NotificationGroupDto} with fields copied from the entity
     */
    public static NotificationGroupDto mapToDto(NotificationGroup group){
        return NotificationGroupDto.builder()
                .id(group.getId())
                .name(group.getName())
                .code(group.getCode())
                .description(group.getDescription())
                .active(group.isActive())
                .createdBy(group.getCreatedBy())
                .createdAt(group.getCreatedAt())
                .updatedBy(group.getUpdatedBy())
                .updatedAt(group.getUpdatedAt())
                .build();
    }
}

