package com.middleware.backend.notification.mapper;

import com.middleware.backend.notification.dto.NotificationTemplateDto;
import com.middleware.backend.notification.enums.ChannelType;
import com.middleware.backend.notification.model.NotificationTemplate;

public class TemplateMapper {
    public static NotificationTemplate MapToEntity(NotificationTemplateDto notification){
        return NotificationTemplate.builder()
                .id(notification.getId())
                .name(notification.getName())
                .code(notification.getCode())
                .type(ChannelType.valueOf(notification.getType()))
                .subject(notification.getSubject())
                .body(notification.getBody())
                .createdBy(notification.getCreatedBy())
                .createdAt(notification.getCreatedAt())
                .updatedBy(notification.getUpdatedBy())
                .updatedAt(notification.getUpdatedAt())
                .build();
    }

    public static NotificationTemplateDto MapToDto(NotificationTemplate notification){
        return NotificationTemplateDto.builder()
                .id(notification.getId())
                .name(notification.getName())
                .code(notification.getCode())
                .type(String.valueOf(notification.getType()))
                .subject(notification.getSubject())
                .body(notification.getBody())
                .createdBy(notification.getCreatedBy())
                .createdAt(notification.getCreatedAt())
                .updatedBy(notification.getUpdatedBy())
                .updatedAt(notification.getUpdatedAt())
                .build();
    }

}
