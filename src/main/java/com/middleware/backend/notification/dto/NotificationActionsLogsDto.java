package com.middleware.backend.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * Data Transfer Object representing an entry in the notification actions logs.
 *
 * <p>This DTO is used to transfer log information related to actions performed
 * on notifications, including action type, details, user email, and timestamp.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationActionsLogsDto {

    /**
     * Unique identifier of the log entry.
     */
    private long id;

    /**
     * Type of action performed (e.g., CREATE, UPDATE, SEND).
     */
    private String action;

    /**
     * Additional details describing the action.
     */
    private String details;

    /**
     * Email of the user who performed the action.
     */
    private String email;

    /**
     * Timestamp when the action occurred.
     */
    private Timestamp eventTime;
}

