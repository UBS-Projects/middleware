package com.middleware.backend.notification.dto;

import com.middleware.backend.notification.enums.ChannelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChannelConfigDto {
    private Long id;
    private String type;   // EMAIL or SMS
    private String name;        // e.g., "Default Email"
    private String config;      // JSON with credentials & api_url
    private boolean active;
    private String createdBy;
    private Timestamp createdAt;
    private String updatedBy;
    private Timestamp updatedAt;
}
