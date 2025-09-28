package com.middleware.backend.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationActionsLogsDto {
    private long id;
    private String action;
    private String details;
    private String email;
    private Timestamp eventTime;
}
