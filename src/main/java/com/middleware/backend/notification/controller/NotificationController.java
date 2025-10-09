package com.middleware.backend.notification.controller;

import com.middleware.backend.notification.dto.NotificationRequest;
import com.middleware.backend.notification.model.NotificationLog;
import com.middleware.backend.notification.service.NotificationService;
import com.middleware.backend.notification.specification.NotificationSpecification;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * Controller responsible for managing notification operations.
 * <p>
 * Provides endpoints for sending notifications to groups
 * and retrieving notification logs with filtering, sorting, and pagination.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Sends a notification immediately to one or more groups.
     *
     * @param request the {@link NotificationRequest} containing group codes, template code, and channel code
     * @return {@link ResponseEntity} containing success or failure message
     */
    @Operation(
            summary = "Send Notification Now",
            description = "Sends a notification immediately to one or more groups using the provided template and channel."
    )
    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendNow(@RequestBody NotificationRequest request) {
        try {
            notificationService.sendToGroup(request.getGroupCodes(), request.getTemplateCode(), request.getChannelCode());
            return ResponseEntity.ok(Map.of("message", "Notification sent successfully"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to send notification: " + ex.getMessage()));
        }
    }

    // ======================= LOGS ENDPOINTS =======================

    /**
     * Retrieves a paginated list of sent notifications with optional filters and sorting.
     *
     * @param groupName     optional filter by group name (partial match)
     * @param templateName  optional filter by template name (partial match)
     * @param channelName   optional filter by channel name (partial match)
     * @param createdAfter  optional filter for notifications sent after this date
     * @param createdBefore optional filter for notifications sent before this date
     * @param page          page number (default 0)
     * @param size          number of records per page (default 10)
     * @param sortedBy      field to sort by (default "sentAt")
     * @param sortDirection sort direction, either "asc" or "desc" (default "desc")
     * @return paginated list of notifications matching the filters
     */
    @Operation(
            summary = "List Notification Logs",
            description = "Retrieves a paginated list of sent notifications with optional filters by group name, template name, channel name, and creation date range. Supports sorting by any field."
    )
    @PreAuthorize("hasAuthority('notification:view')")
    @GetMapping("")
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) String groupName,
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) String channelName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "sentAt") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        Pageable pageable = PageRequest.of(page, size,
                sortDirection.equalsIgnoreCase("asc")
                        ? Sort.by(sortedBy).ascending()
                        : Sort.by(sortedBy).descending());

        Specification<NotificationLog> spec = Specification
                .where(NotificationSpecification.hasField("groupName", groupName, NotificationSpecification.MatchMode.CONTAINS))
                .and(NotificationSpecification.hasField("templateName", templateName, NotificationSpecification.MatchMode.CONTAINS))
                .and(NotificationSpecification.hasField("channelName", channelName, NotificationSpecification.MatchMode.CONTAINS))
                .and(NotificationSpecification.dateAfter("createdAt", createdAfter))
                .and(NotificationSpecification.dateBefore("createdAt", createdBefore));

        return ResponseEntity.ok(notificationService.findAll(spec, pageable));
    }

    /**
     * Retrieves a specific notification log by ID.
     *
     * @param id the ID of the notification log
     * @return {@link ResponseEntity} containing the notification log details
     */
    @Operation(
            summary = "Get Notification Log by ID",
            description = "Fetches a single notification log entry by its unique identifier."
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('notification:view')")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return notificationService.getById(id);
    }
}
