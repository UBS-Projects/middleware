package com.middleware.backend.notification.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDto {
    private Long id;
    private String category;
    private String severity;
    private String messageTemplate;
    private String channel;
    private String status;
    private String relatedEntityType;
    private Long relatedEntityId;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
