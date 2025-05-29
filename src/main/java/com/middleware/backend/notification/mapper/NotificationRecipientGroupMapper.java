package com.middleware.backend.notification.mapper;

import com.middleware.backend.notification.dto.*;
import com.middleware.backend.notification.model.*;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationRecipientGroupMapper {
    NotificationRecipientGroupDto toDto(NotificationRecipientGroup entity);

    NotificationRecipientGroup toEntity(NotificationRecipientGroupDto dto);
}
