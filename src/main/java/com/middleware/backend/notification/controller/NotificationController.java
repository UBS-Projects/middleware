package com.middleware.backend.notification.controller;

import com.middleware.backend.notification.dto.NotificationRequest;
import com.middleware.backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendNow(@RequestBody NotificationRequest request) {
        try {
            notificationService.sendToGroup(request.getGroupId(), request.getTemplateId(), request.getChannelId());
            return ResponseEntity.ok(Map.of("message", "Notification sent successfully"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to send notification: " + ex.getMessage()));
        }
    }


    @PostMapping("/schedule")
    public String scheduleSend(@RequestParam List<Long> groupId,
                               @RequestParam Long templateId,
                               @RequestParam Long channelId,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime sendTime) {

        notificationService.scheduleSend(groupId, templateId, channelId, sendTime);
        return "Notification scheduled for " + sendTime;
    }


//Logs Endpoints
    @GetMapping("")
    public ResponseEntity<?> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "sentAt") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        Pageable pageable = PageRequest.of(page, size,
                sortDirection.equalsIgnoreCase("asc") ? Sort.by(sortedBy).ascending() : Sort.by(sortedBy).descending());

        return ResponseEntity.ok(notificationService.findAll(pageable));
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return notificationService.getById(id);
    }
}
