package com.middleware.backend.notification.mapper;

import com.middleware.backend.notification.dto.ChannelConfigDto;
import com.middleware.backend.notification.enums.ChannelType;
import com.middleware.backend.notification.model.ChannelConfig;

public class ChannelMapper {
    public static ChannelConfig mapToEntity(ChannelConfigDto channel){
        return ChannelConfig.builder()
                .id(channel.getId())
                .type(ChannelType.valueOf(channel.getType()))
                .name(channel.getName())
                .config(channel.getConfig())
                .active(channel.isActive())
                .createdBy(channel.getCreatedBy())
                .createdAt(channel.getCreatedAt())
                .updatedBy(channel.getUpdatedBy())
                .updatedAt(channel.getUpdatedAt())
                .build();
    }


    public static ChannelConfigDto mapToDto(ChannelConfig channel){
        return ChannelConfigDto.builder()
                .id(channel.getId())
                .type(String.valueOf(channel.getType()))
                .name(channel.getName())
                .config(channel.getConfig())
                .active(channel.isActive())
                .createdBy(channel.getCreatedBy())
                .createdAt(channel.getCreatedAt())
                .updatedBy(channel.getUpdatedBy())
                .updatedAt(channel.getUpdatedAt())
                .build();
    }
}
