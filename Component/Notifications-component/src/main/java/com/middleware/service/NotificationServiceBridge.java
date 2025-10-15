package com.middleware.service;

import java.util.List;

/**
 * Bridge interface to delegate notification sending logic
 * from the Camel component to the backend NotificationService.
 */
public interface NotificationServiceBridge {

    /**
     * Default bean ID for the NotificationService.
     */
    String BEAN_ID = "notificationService";

    /**
     * Sends a notification to a group of recipients.
     *
     * @param groupCodes   Codes of the target groups.
     * @param templateCode Code of the notification template.
     * @param channelCode  Code of the channel to use for sending.
     */
    void sendToGroup(List<String> groupCodes, String templateCode, String channelCode);
}