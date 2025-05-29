package com.middleware.backend.notification.mapper;

import com.middleware.backend.notification.dto.*;
import com.middleware.backend.notification.model.*;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    NotificationDto toDto(Notification entity);

    Notification toEntity(NotificationDto dto);
}
