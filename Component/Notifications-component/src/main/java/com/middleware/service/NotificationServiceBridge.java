package com.middleware.service;

import java.util.List;

/**
 * Bridge interface to delegate notification sending logic
 * from the Camel component to the backend NotificationService.
 */
public interface NotificationServiceBridge {

    String BEAN_ID = "notificationService";

    void sendToGroup(List<String> groupCodes, String templateCode, String channelCode);
}
