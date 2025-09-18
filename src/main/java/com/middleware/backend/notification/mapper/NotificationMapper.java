//package com.middleware.backend.notification.mapper;
//
//import com.middleware.backend.notification.dto.NotificationDto;
//import com.middleware.backend.notification.model.ChannelConfig;
//import com.middleware.backend.notification.model.Notification;
//import com.middleware.backend.notification.model.NotificationTemplate;
//import com.middleware.backend.notification.model.Receiver;
//
//import java.sql.Timestamp;
//import java.util.List;
//import java.util.stream.Collectors;
//
//public class NotificationMapper {
//
//    public static Notification mapToEntity(NotificationDto dto, NotificationTemplate template,
//                                           ChannelConfig channel, List<Receiver> receivers, String currentUser) {
//        Timestamp now = new Timestamp(System.currentTimeMillis());
//
//        return Notification.builder()
//                .id(dto.getId())
//                .subject(dto.getSubject())
//                .body(dto.getBody())
//                .template(template)
//                .channel(channel)
//                .receivers(receivers)
//                .status(dto.getStatus() != null ? dto.getStatus() : com.middleware.backend.notification.enums.NotificationStatus.PENDING)
//                .scheduledAt(dto.getScheduledAt())
//                .sentAt(dto.getSentAt())
//                .createdBy(currentUser)
//                .createdAt(now)
//                .updatedBy(currentUser)
//                .updatedAt(now)
//                .build();
//    }
//
//    public static NotificationDto mapToDto(Notification notification) {
//        return NotificationDto.builder()
//                .id(notification.getId())
//                .subject(notification.getSubject())
//                .body(notification.getBody())
//                .templateId(notification.getTemplate() != null ? notification.getTemplate().getId() : null)
//                .channelId(notification.getChannel() != null ? notification.getChannel().getId() : null)
//                .receiverIds(notification.getReceivers() != null
//                        ? notification.getReceivers().stream().map(Receiver::getId).collect(Collectors.toList())
//                        : null)
//                .status(notification.getStatus())
//                .scheduledAt(notification.getScheduledAt())
//                .sentAt(notification.getSentAt())
//                .createdBy(notification.getCreatedBy())
//                .createdAt(notification.getCreatedAt())
//                .updatedBy(notification.getUpdatedBy())
//                .updatedAt(notification.getUpdatedAt())
//                .build();
//    }
//}
