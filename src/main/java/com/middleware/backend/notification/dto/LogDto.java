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

/**
 * Data Transfer Object representing a notification log entry.
 *
 * <p>This DTO is used to transfer log information between the backend service and client,
 * including details about the notification group, template, channel, status, and timestamps.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LogDto {

    /**
     * Unique identifier of the log entry.
     */
    private Long id;

    /**
     * Name of the notification group associated with this log.
     */
    private String groupName;

    /**
     * Name of the notification template used.
     */
    private String templateName;

    /**
     * Name of the notification channel used (e.g., EMAIL, SMS).
     */
    private String channelName;

    /**
     * Status of the notification (e.g., SENT, FAILED).
     */
    private String status;

    /**
     * Error message if the notification failed to send.
     */
    private String errorMessage;

    /**
     * Username of the user who triggered the notification.
     */
    private String userName;


    /**
     * The body content of the notification request.
     */
    private String requestBody;

    /**
     * The headers included in the notification request.
     */
    private String requestHeader;

    /**
     * The body content received in the notification response.
     */
    private String responseBody;

    /**
     * The headers received in the notification response.
     */
    private String responseHeader;

    /**
     * The HTTP status code returned in the notification response.
     */
    private String responseCode;



    /**
     * Timestamp when the log entry was created.
     */
    private Timestamp createdAt;

    /**
     * Timestamp when the notification was sent.
     */
    private Timestamp sentAt;
}

