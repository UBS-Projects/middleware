package com.middleware.backend.notification.config;

import com.middleware.backend.notification.service.NotificationService;
import com.middleware.service.NotificationServiceBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Bridge to expose backend NotificationService to Camel component.
 */
@Configuration
public class NotificationBridgeConfiguration {

    private final NotificationService backendNotificationService;

    public NotificationBridgeConfiguration(NotificationService backendNotificationService) {
        this.backendNotificationService = backendNotificationService;
    }

    @Bean(name = NotificationServiceBridge.BEAN_ID)
    public NotificationServiceBridge notificationServiceBridge() {
        return new NotificationServiceBridge() {
            @Override
            public void sendToGroup(List<String> groupCodes, String templateCode, String channelCode) {
                backendNotificationService.sendToGroup(groupCodes, templateCode, channelCode);
            }

        };
    }
}