package com.middleware.backend.notification.mapper;

import com.middleware.backend.notification.dto.ReceiverDto;
import com.middleware.backend.notification.model.Receiver;

public class ReceiverMapper {
    public static Receiver mapToEntity(ReceiverDto receiver){
        return Receiver.builder()
                .id(receiver.getId())
                .name(receiver.getName())
                .email(receiver.getEmail())
                .phone(receiver.getPhone())
                .active(receiver.isActive())
                .createdBy(receiver.getCreatedBy())
                .createdAt(receiver.getCreatedAt())
                .updatedBy(receiver.getUpdatedBy())
                .updatedAt(receiver.getUpdatedAt())
                .build();
    }



    public static ReceiverDto mapToDto(Receiver receiver){
        return ReceiverDto.builder()
                .id(receiver.getId())
                .name(receiver.getName())
                .email(receiver.getEmail())
                .phone(receiver.getPhone())
                .active(receiver.isActive())
                .createdBy(receiver.getCreatedBy())
                .createdAt(receiver.getCreatedAt())
                .updatedBy(receiver.getUpdatedBy())
                .updatedAt(receiver.getUpdatedAt())
                .build();
    }
}
