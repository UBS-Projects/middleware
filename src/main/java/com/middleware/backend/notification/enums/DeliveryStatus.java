package com.middleware.backend.notification.enums;

/**
 * Enum representing the delivery status of a notification.
 *
 * <p>Indicates whether a notification is pending, successfully sent, or failed.</p>
 */
public enum DeliveryStatus {

    /**
     * Notification is pending and has not been sent yet.
     */
    PENDING,

    /**
     * Notification was successfully sent.
     */
    SENT,

    /**
     * Notification failed to be sent.
     */
    FAILED
}
