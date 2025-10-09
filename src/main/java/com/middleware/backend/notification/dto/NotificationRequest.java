package com.middleware.backend.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object representing a request to send a notification.
 *
 * <p>This DTO is used to carry information from the client to the backend service
 * when triggering notifications to one or more groups through a specific template
 * and channel.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationRequest {

    /**
     * List of notification group codes to which the notification will be sent.
     */
    private List<String> groupCodes;

    /**
     * Code of the notification template to use.
     */
    private String templateCode;

    /**
     * Code of the channel through which the notification will be sent
     * (e.g., EMAIL, SMS, PUSH).
     */
    private String channelCode;
}
