package com.middleware.backend.notification.controller;

import com.middleware.backend.notification.model.NotificationActionsLogs;
import com.middleware.backend.notification.service.NotificationActionsLogsService;
import com.middleware.backend.notification.specification.NotificationActionsLogsConfigSpecification;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/notification-logs")
@AllArgsConstructor
public class NotificationActionsLogsController {

    private final NotificationActionsLogsService service;

    /**
     * Retrieves a paginated list of notification action logs with optional filters.
     *
     * @param action contains match on notification action (e.g., SENT, FAILED)
     * @param email contains match on user email address
     * @param details contains match on log details
     * @param createdAfter filter logs with event time on/after this date
     * @param createdBefore filter logs with event time on/before this date
     * @param page zero-based page index
     * @param size page size
     * @param sortedBy property to sort by (default "eventTime")
     * @param sortDirection sort order: "asc" or "desc"
     * @return 200 with a paginated list of notification logs matching criteria
     */
    @GetMapping("")
    @PreAuthorize("hasAuthority('notificationLogs:view')")
    @Operation(
            summary = "List notification logs",
            description = "Retrieves a paginated list of notification action logs with optional filters by action, email, details, and date range. Supports sorting and pagination."
    )
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String details,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "eventTime") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        Pageable pageable = PageRequest.of(page, size,
                sortDirection.equalsIgnoreCase("asc")
                        ? Sort.by(sortedBy).ascending()
                        : Sort.by(sortedBy).descending());

        Specification<NotificationActionsLogs> spec = Specification
                .where(NotificationActionsLogsConfigSpecification.hasField("action", action, NotificationActionsLogsConfigSpecification.MatchMode.CONTAINS))
                .and(NotificationActionsLogsConfigSpecification.hasField("email", email, NotificationActionsLogsConfigSpecification.MatchMode.CONTAINS))
                .and(NotificationActionsLogsConfigSpecification.hasField("details", details, NotificationActionsLogsConfigSpecification.MatchMode.CONTAINS))
                .and(NotificationActionsLogsConfigSpecification.dateAfter("eventTime", createdAfter))
                .and(NotificationActionsLogsConfigSpecification.dateBefore("eventTime", createdBefore));

        return ResponseEntity.ok(service.findAll(spec, pageable));
    }

    /**
     * Retrieves a specific notification log entry by its ID.
     *
     * @param id unique notification log ID
     * @return 200 with log details or 404 if not found
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('notificationLogs:view')")
    @Operation(
            summary = "Get notification log by ID",
            description = "Retrieves detailed information about a specific notification action log by its unique ID."
    )
    public ResponseEntity<?> get(@PathVariable long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    /**
     * Exports filtered notification logs to a CSV or XLSX file.
     *
     * @param action contains match on notification action
     * @param email contains match on user email
     * @param details contains match on log details
     * @param createdAfter filter logs with event time on/after this date
     * @param createdBefore filter logs with event time on/before this date
     * @param sortedBy property to sort by (default "eventTime")
     * @param sortDirection sort order: "asc" or "desc"
     * @param type export file type ("CSV" or "XLSX")
     * @return 200 with downloadable file or 500 on server error
     */
    @GetMapping("/export/{type}")
    @PreAuthorize("hasAuthority('notificationLogs:export')")
    @Operation(
            summary = "Export notification logs",
            description = "Exports notification action logs to a CSV or XLSX file with optional filters and sorting."
    )
    public ResponseEntity<byte[]> exportFile(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String details,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdBefore,
            @RequestParam(defaultValue = "eventTime") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @PathVariable("type") String type
    ) {
        try {
            Specification<NotificationActionsLogs> spec = Specification
                    .where(NotificationActionsLogsConfigSpecification.hasField("action", action, NotificationActionsLogsConfigSpecification.MatchMode.CONTAINS))
                    .and(NotificationActionsLogsConfigSpecification.hasField("email", email, NotificationActionsLogsConfigSpecification.MatchMode.CONTAINS))
                    .and(NotificationActionsLogsConfigSpecification.hasField("details", details, NotificationActionsLogsConfigSpecification.MatchMode.CONTAINS))
                    .and(NotificationActionsLogsConfigSpecification.dateAfter("eventTime", createdAfter))
                    .and(NotificationActionsLogsConfigSpecification.dateBefore("eventTime", createdBefore));

            byte[] fileBytes = service.exportFile(spec, type, sortedBy,sortDirection);

            if (fileBytes == null || fileBytes.length == 0) {
                return ResponseEntity.noContent().build();
            }

            String fileName = "notification_logs." + (type.equalsIgnoreCase("CSV") ? "csv" : "xlsx");
            String contentType = type.equalsIgnoreCase("CSV")
                    ? "text/csv"
                    : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(fileBytes);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
