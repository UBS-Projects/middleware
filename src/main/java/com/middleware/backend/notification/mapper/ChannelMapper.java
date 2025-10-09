package com.middleware.backend.notification.mapper;

import com.middleware.backend.notification.dto.ChannelConfigDto;
import com.middleware.backend.notification.enums.ChannelType;
import com.middleware.backend.notification.model.ChannelConfig;

/**
 * Mapper class for converting between {@link ChannelConfig} entities and {@link ChannelConfigDto} DTOs.
 *
 * <p>This class provides static methods to map data between the entity and DTO layers,
 * ensuring a consistent representation of channel configuration data across the application.</p>
 */
public class ChannelMapper {

    /**
     * Converts a {@link ChannelConfigDto} to a {@link ChannelConfig} entity.
     *
     * @param channel the DTO to convert
     * @return a {@link ChannelConfig} entity with fields copied from the DTO
     * @throws IllegalArgumentException if the type in the DTO does not match a valid {@link ChannelType}
     */
    public static ChannelConfig mapToEntity(ChannelConfigDto channel){
        return ChannelConfig.builder()
                .id(channel.getId())
                .type(ChannelType.valueOf(channel.getType()))
                .name(channel.getName())
                .code(channel.getCode())
                .config(channel.getConfig())
                .active(channel.isActive())
                .createdBy(channel.getCreatedBy())
                .createdAt(channel.getCreatedAt())
                .updatedBy(channel.getUpdatedBy())
                .updatedAt(channel.getUpdatedAt())
                .build();
    }

    /**
     * Converts a {@link ChannelConfig} entity to a {@link ChannelConfigDto}.
     *
     * @param channel the entity to convert
     * @return a {@link ChannelConfigDto} with fields copied from the entity
     */
    public static ChannelConfigDto mapToDto(ChannelConfig channel){
        return ChannelConfigDto.builder()
                .id(channel.getId())
                .type(String.valueOf(channel.getType()))
                .name(channel.getName())
                .code(channel.getCode())
                .config(channel.getConfig())
                .active(channel.isActive())
                .createdBy(channel.getCreatedBy())
                .createdAt(channel.getCreatedAt())
                .updatedBy(channel.getUpdatedBy())
                .updatedAt(channel.getUpdatedAt())
                .build();
    }
}

