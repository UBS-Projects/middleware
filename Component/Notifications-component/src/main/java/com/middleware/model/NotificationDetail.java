package com.middleware.model;

import lombok.Getter;
import lombok.Setter;

/**
 * A data transfer object for notification details.
 * Contains the group code, template code, and channel code
 * for sending a notification.
 */
@Getter
@Setter
public class NotificationDetail {
    /**
     * The code of the group to receive the notification.
     */
    private String groupCode;

    /**
     * The code of the notification template to use.
     */
    private String templateCode;

    /**
     * The code of the channel (e.g., email, sms) to send the notification through.
     */
    private String channelCode;
}