package com.middleware.backend.notification.dto;

import com.middleware.backend.notification.model.ChannelConfig;
import com.middleware.backend.notification.model.NotificationGroup;
import com.middleware.backend.notification.model.NotificationTemplate;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LogDto {
    private Long id;
    private String groupName;
    private String templateName;
    private String channelName;
    private String status;
    private String errorMessage;
    private String userName;
    private Timestamp createdAt;
    private Timestamp sentAt;

}
