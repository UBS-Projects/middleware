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
 * Entity representing a log of actions performed on the notification system.
 *
 * <p>This entity stores details about each action, including the type of action,
 * details of the operation, the affected user's email, and the timestamp of the event.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "notification_action_log")
public class NotificationActionsLogs {

    /**
     * Unique identifier for the log entry.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    /**
     * The action performed, e.g., "SEND_NOTIFICATION", "UPDATE_TEMPLATE".
     */
    private String action;

    /**
     * Additional details about the action performed.
     */
    private String details;

    /**
     * Email of the user affected by the action.
     */
    private String email;

    /**
     * Timestamp when the action occurred.
     */
    private Timestamp eventTime;
}
