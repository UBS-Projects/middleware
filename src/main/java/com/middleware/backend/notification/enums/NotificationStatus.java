package com.middleware.backend.notification.enums;

/**
 * Enum representing the status of a notification.
 *
 * <p>Used to track the current state of a notification throughout its lifecycle.</p>
 */
public enum NotificationStatus {

    /**
     * Notification is pending and has not been processed yet.
     */
    PENDING,

    /**
     * Notification was successfully sent.
     */
    SENT,

    /**
     * Notification failed to be sent.
     */
    FAILED,

    /**
     * Notification was cancelled and will not be sent.
     */
    CANCELLED
}
