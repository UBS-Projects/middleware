package com.middleware.backend.notification.service.SMS;

import com.middleware.backend.notification.model.Receiver;

import java.util.Map;

public interface SmsProvider {
    void send(Receiver receiver, String messageText, Map<String, String> config) throws Exception;
}