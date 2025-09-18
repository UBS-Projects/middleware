package com.middleware.backend.notification.service;

import java.time.LocalDateTime;
import java.util.Map;

public interface NotificationService {
    void sendToGroup(Long groupId, Long templateId, Long channelId);
    void scheduleSend(Long groupId, Long templateId, Long channelId, LocalDateTime sendTime);
}

