package com.middleware.backend.notification.dto;

import com.middleware.backend.notification.enums.ChannelType;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationTemplateDto {
    private Long id;
    private String name;          // e.g., Password Reset
    private String type;     // SMS, EMAIL
    private String subject;       // For email templates
    private String body;
    private String createdBy;
    private Timestamp createdAt;
    private String updatedBy;
    private Timestamp updatedAt;
}
