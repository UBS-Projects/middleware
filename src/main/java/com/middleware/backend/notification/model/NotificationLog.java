package com.middleware.backend.notification.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;

/**
 * Entity representing a notification log entry.
 *
 * <p>This entity stores detailed information about a notification sent through the system,
 * including the group, template, and channel used, status, error messages (if any),
 * the user who triggered the notification, and timestamps for creation and sending.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "notification_logs")
public class NotificationLog {

    /**
     * Unique identifier for the notification log entry.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The notification group associated with this log entry.
     */
    @Column(name = "notification_group")
    private String group;

    /**
     * The notification template used for this notification.
     */
    private String template;

    /**
     * The channel through which the notification was sent.
     */
    private String channel;

    /**
     * Status of the notification (e.g., PENDING, SUCCESS, FAILED).
     */
    private String status;

    /**
     * Error message if the notification failed.
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Name of the user who triggered the notification.
     */
    @Column(name = "user_name")
    private String userName;




    /**
     * The body content of the notification request.
     */
    @Column(name = "request_body",columnDefinition = "TEXT")
    private String requestBody;

    /**
     * The headers included in the notification request.
     */
    @Column(name = "request_header",columnDefinition = "TEXT")
    private String requestHeader;

    /**
     * The body content received in the notification response.
     */
    @Column(name = "response_body",columnDefinition = "TEXT")
    private String responseBody;

    /**
     * The headers received in the notification response.
     */
    @Column(name = "response_header",columnDefinition = "TEXT")
    private String responseHeader;

    /**
     * The HTTP status code returned in the notification response.
     */
    @Column(name = "response_code",columnDefinition = "TEXT")
    private String responseCode;


    /**
     * Timestamp when the notification log entry was created.
     */
    @Column(name="created_at")
    private Timestamp createdAt;

    /**
     * Timestamp when the notification was sent.
     */
    @Column(name="sent_at")
    private Timestamp sentAt;
}
