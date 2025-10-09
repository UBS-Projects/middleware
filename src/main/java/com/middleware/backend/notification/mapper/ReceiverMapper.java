package com.middleware.backend.notification.mapper;

import com.middleware.backend.notification.dto.ReceiverDto;
import com.middleware.backend.notification.model.Receiver;

/**
 * Mapper class for converting between {@link Receiver} entities and {@link ReceiverDto} DTOs.
 *
 * <p>This class provides static methods to map data between the entity and DTO layers,
 * ensuring consistent representation of receiver information across the application.</p>
 */
public class ReceiverMapper {

    /**
     * Converts a {@link ReceiverDto} to a {@link Receiver} entity.
     *
     * @param receiver the DTO to convert
     * @return a {@link Receiver} entity with fields copied from the DTO
     */
    public static Receiver mapToEntity(ReceiverDto receiver){
        return Receiver.builder()
                .id(receiver.getId())
                .name(receiver.getName())
                .email(receiver.getEmail())
                .phone(receiver.getPhone())
                .createdBy(receiver.getCreatedBy())
                .createdAt(receiver.getCreatedAt())
                .updatedBy(receiver.getUpdatedBy())
                .updatedAt(receiver.getUpdatedAt())
                .build();
    }

    /**
     * Converts a {@link Receiver} entity to a {@link ReceiverDto}.
     *
     * @param receiver the entity to convert
     * @return a {@link ReceiverDto} with fields copied from the entity
     */
    public static ReceiverDto mapToDto(Receiver receiver){
        return ReceiverDto.builder()
                .id(receiver.getId())
                .name(receiver.getName())
                .email(receiver.getEmail())
                .phone(receiver.getPhone())
                .createdBy(receiver.getCreatedBy())
                .createdAt(receiver.getCreatedAt())
                .updatedBy(receiver.getUpdatedBy())
                .updatedAt(receiver.getUpdatedAt())
                .build();
    }
}
