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


/**
 * Data Transfer Object representing a notification template.
 *
 * <p>This DTO is used to transfer template information between the client and
 * backend service, including template metadata, type, content, and audit information.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationTemplateDto {

    /**
     * Unique identifier of the notification template.
     */
    private Long id;

    /**
     * Human-readable name of the template.
     * Example: "Password Reset".
     */
    @NotBlank(message = "Name is required")
    private String name;

    /**
     * Unique code representing the template.
     */
    private String code;

    /**
     * Type of the template. Must be either "SMS" or "EMAIL".
     */
    @NotBlank(message = "Type is required")
    @Pattern(regexp = "SMS|EMAIL", message = "Type must be either SMS or EMAIL")
    private String type;

    /**
     * Subject of the template, used for email notifications.
     */
    @NotBlank(message = "Subject is required")
    private String subject;

    /**
     * Body content of the notification template.
     */
    @NotBlank(message = "Body is required")
    private String body;

    /**
     * Flag indicating whether the template is currently active.
     */
    private boolean active;

    /**
     * Username or identifier of the user who created the template.
     */
    private String createdBy;

    /**
     * Timestamp when the template was created.
     */
    private Timestamp createdAt;

    /**
     * Username or identifier of the user who last updated the template.
     */
    private String updatedBy;

    /**
     * Timestamp when the template was last updated.
     */
    private Timestamp updatedAt;
}
