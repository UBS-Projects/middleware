package com.middleware.backend.notification.controller;

import com.middleware.backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/send")
    public String sendNow(@RequestParam Long groupId,
                          @RequestParam Long templateId,
                          @RequestParam Long channelId) {

        notificationService.sendToGroup(groupId, templateId, channelId);
        return "Notification sent immediately";
    }

    @PostMapping("/schedule")
    public String scheduleSend(@RequestParam Long groupId,
                               @RequestParam Long templateId,
                               @RequestParam Long channelId,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime sendTime) {

        notificationService.scheduleSend(groupId, templateId, channelId, sendTime);
        return "Notification scheduled for " + sendTime;
    }
}
