//package com.middleware.backend.notification.dto;
//
//import com.middleware.backend.notification.enums.NotificationStatus;
//import lombok.*;
//
//import java.sql.Timestamp;
//import java.util.List;
//
//@Data
//@AllArgsConstructor
//@NoArgsConstructor
//@Builder
//public class NotificationDto {
//    private Long id;
//    private String subject;
//    private String body;
//    private Long templateId;
//    private Long channelId;
//    private Long groupId;              // optional group
//    private List<Long> receiverIds;    // optional individual receivers
//    private NotificationStatus status;
//    private Timestamp scheduledAt;
//    private Timestamp sentAt;
//    private String createdBy;
//    private Timestamp createdAt;
//    private String updatedBy;
//    private Timestamp updatedAt;
//}