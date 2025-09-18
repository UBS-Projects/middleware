package com.middleware.backend.notification.dto;

import com.middleware.backend.notification.enums.ChannelType;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

    @NotBlank(message = "Name is required")
    private String name;          // e.g., Password Reset

    @NotBlank(message = "Type is required")
    @Pattern(regexp = "SMS|EMAIL", message = "Type must be either SMS or EMAIL")
    private String type;     // SMS, EMAIL

    @NotBlank(message = "Subject is required")
    private String subject;       // For email templates

    @NotBlank(message = "Body is required")
    private String body;

    private String createdBy;
    private Timestamp createdAt;
    private String updatedBy;
    private Timestamp updatedAt;
}