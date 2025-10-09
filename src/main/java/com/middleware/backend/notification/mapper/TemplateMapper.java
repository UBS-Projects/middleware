package com.middleware.backend.notification.mapper;

import com.middleware.backend.notification.dto.NotificationTemplateDto;
import com.middleware.backend.notification.enums.ChannelType;
import com.middleware.backend.notification.model.NotificationTemplate;

/**
 * Mapper class for converting between {@link NotificationTemplate} entities and {@link NotificationTemplateDto} DTOs.
 *
 * <p>This class provides static methods to map data between the entity and DTO layers,
 * ensuring consistent representation of notification template information across the application.</p>
 */
public class TemplateMapper {

    /**
     * Converts a {@link NotificationTemplateDto} to a {@link NotificationTemplate} entity.
     *
     * @param notification the DTO to convert
     * @return a {@link NotificationTemplate} entity with fields copied from the DTO
     * @throws IllegalArgumentException if the type in the DTO does not match a valid {@link ChannelType}
     */
    public static NotificationTemplate MapToEntity(NotificationTemplateDto notification){
        return NotificationTemplate.builder()
                .id(notification.getId())
                .name(notification.getName())
                .code(notification.getCode())
                .type(ChannelType.valueOf(notification.getType()))
                .subject(notification.getSubject())
                .body(notification.getBody())
                .active(notification.isActive())
                .createdBy(notification.getCreatedBy())
                .createdAt(notification.getCreatedAt())
                .updatedBy(notification.getUpdatedBy())
                .updatedAt(notification.getUpdatedAt())
                .build();
    }

    /**
     * Converts a {@link NotificationTemplate} entity to a {@link NotificationTemplateDto}.
     *
     * @param notification the entity to convert
     * @return a {@link NotificationTemplateDto} with fields copied from the entity
     */
    public static NotificationTemplateDto MapToDto(NotificationTemplate notification){
        return NotificationTemplateDto.builder()
                .id(notification.getId())
                .name(notification.getName())
                .code(notification.getCode())
                .type(String.valueOf(notification.getType()))
                .subject(notification.getSubject())
                .body(notification.getBody())
                .active(notification.isActive())
                .createdBy(notification.getCreatedBy())
                .createdAt(notification.getCreatedAt())
                .updatedBy(notification.getUpdatedBy())
                .updatedAt(notification.getUpdatedAt())
                .build();
    }
}

