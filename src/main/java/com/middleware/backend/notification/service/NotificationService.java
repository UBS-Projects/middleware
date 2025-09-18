//package com.middleware.backend.notification.service;
//
//import com.middleware.backend.notification.dto.NotificationDto;
//import com.middleware.backend.notification.mapper.NotificationMapper;
//import com.middleware.backend.notification.model.ChannelConfig;
//import com.middleware.backend.notification.model.Notification;
//import com.middleware.backend.notification.model.NotificationTemplate;
//import com.middleware.backend.notification.model.Receiver;
//import com.middleware.backend.notification.repository.*;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class NotificationService {
//
//    private final NotificationRepository notificationRepo;
//    private final TemplateRepository templateRepo;
//    private final ChannelConfigRepository channelRepo;
//    private final NotificationGroupRepository groupRepo;
//    private final ReceiverRepository receiverRepo;
//
//    public NotificationDto send(NotificationDto dto) {
//
//        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
//
//        NotificationTemplate template = templateRepo.findById(dto.getTemplateId())
//                .orElseThrow(() -> new RuntimeException("Template not found"));
//
//        ChannelConfig channel = channelRepo.findById(dto.getChannelId())
//                .orElseThrow(() -> new RuntimeException("Channel not found"));
//
//        List<Receiver> receivers;
//
//        if (dto.getGroupId() != null) {
//            receivers = groupRepo.findById(dto.getGroupId())
//                    .orElseThrow(() -> new RuntimeException("Group not found"))
//                    .getReceivers();
//        } else if (dto.getReceiverIds() != null && !dto.getReceiverIds().isEmpty()) {
//            receivers = receiverRepo.findAllById(dto.getReceiverIds());
//        } else {
//            throw new RuntimeException("No receivers specified");
//        }
//
//        Notification notification = NotificationMapper.mapToEntity(dto, template, channel, receivers, currentUser);
//        notificationRepo.save(notification);
//
//        // TODO: Implement actual sending logic (SMS/email) async
//
//        return NotificationMapper.mapToDto(notification);
//    }
//
//    public NotificationDto findById(Long id) {
//        return notificationRepo.findById(id)
//                .map(NotificationMapper::mapToDto)
//                .orElseThrow(() -> new RuntimeException("Notification not found"));
//    }
//
//    public Page<NotificationDto> findAll(Pageable pageable) {
//        return notificationRepo.findAll(pageable)
//                .map(NotificationMapper::mapToDto);
//    }
//}
