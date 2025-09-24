package com.middleware.backend.notification.mapper;

import com.middleware.backend.notification.dto.NotificationGroupDto;
import com.middleware.backend.notification.model.NotificationGroup;

import java.util.stream.Collectors;

public class GroupMapper {
    public static NotificationGroup mapToEntity(NotificationGroupDto group){
        return NotificationGroup.builder()
                .id(group.getId())
                .name(group.getName())
                .code(group.getCode())
                .description(group.getDescription())
                .createdBy(group.getCreatedBy())
                .createdAt(group.getCreatedAt())
                .updatedBy(group.getUpdatedBy())
                .updatedAt(group.getUpdatedAt())
                .build();
    }


    public static NotificationGroupDto mapToDto(NotificationGroup group){
        return NotificationGroupDto.builder()
                .id(group.getId())
                .name(group.getName())
                .code(group.getCode())
                .description(group.getDescription())
                .createdBy(group.getCreatedBy())
                .createdAt(group.getCreatedAt())
                .updatedBy(group.getUpdatedBy())
                .updatedAt(group.getUpdatedAt())
                .build();
    }
}
