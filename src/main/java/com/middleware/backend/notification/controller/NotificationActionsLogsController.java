package com.middleware.backend.notification.controller;

import com.middleware.backend.notification.enums.ChannelType;
import com.middleware.backend.notification.model.ChannelConfig;
import com.middleware.backend.notification.model.NotificationActionsLogs;
import com.middleware.backend.notification.service.NotificationActionsLogsService;
import com.middleware.backend.notification.specification.ChannelConfigSpecification;
import com.middleware.backend.notification.specification.NotificationActionsLogsConfigSpecification;
import com.middleware.backend.scheduledJobs.enums.Status;
import com.middleware.backend.scheduledJobs.model.JobExecutionLogs;
import com.middleware.backend.scheduledJobs.specification.JobExecutionLogsSpecification;
import io.swagger.v3.oas.annotations.Operation;
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

import java.time.LocalDate;

@RestController
@RequestMapping("/api/notification-logs")
@AllArgsConstructor
public class NotificationActionsLogsController {
    private final NotificationActionsLogsService service;

    @GetMapping("")
    @PreAuthorize("hasAuthority('notificationLogs:view')")
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
                sortDirection.equalsIgnoreCase("asc") ? Sort.by(sortedBy).ascending() : Sort.by(sortedBy).descending());


        Specification<NotificationActionsLogs> spec = Specification
                .where(NotificationActionsLogsConfigSpecification.hasField("action", action, NotificationActionsLogsConfigSpecification.MatchMode.CONTAINS))
                .and(NotificationActionsLogsConfigSpecification.hasField("email", email, NotificationActionsLogsConfigSpecification.MatchMode.CONTAINS))
                .and(NotificationActionsLogsConfigSpecification.hasField("details", details, NotificationActionsLogsConfigSpecification.MatchMode.CONTAINS))
                .and(NotificationActionsLogsConfigSpecification.dateAfter("eventTime", createdAfter))
                .and(NotificationActionsLogsConfigSpecification.dateBefore("eventTime", createdBefore));

        return ResponseEntity.ok(service.findAll(spec, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('notificationLogs:view')")
    public ResponseEntity<?> get(@PathVariable long id){
        return ResponseEntity.ok(service.getById(id));
    }




    @GetMapping("/export/{type}")
    @PreAuthorize("hasAuthority('notificationLogs:export')")
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
            Pageable pageable = PageRequest.of(0, 100000,
                    sortDirection.equalsIgnoreCase("asc") ? Sort.by(sortedBy).ascending() : Sort.by(sortedBy).descending());


            Specification<NotificationActionsLogs> spec = Specification
                    .where(NotificationActionsLogsConfigSpecification.hasField("action", action, NotificationActionsLogsConfigSpecification.MatchMode.CONTAINS))
                    .and(NotificationActionsLogsConfigSpecification.hasField("email", email, NotificationActionsLogsConfigSpecification.MatchMode.CONTAINS))
                    .and(NotificationActionsLogsConfigSpecification.hasField("details", details, NotificationActionsLogsConfigSpecification.MatchMode.CONTAINS))
                    .and(NotificationActionsLogsConfigSpecification.dateAfter("eventTime", createdAfter))
                    .and(NotificationActionsLogsConfigSpecification.dateBefore("eventTime", createdBefore));



            byte[] fileBytes = service.exportFile(spec, pageable, type);

            String fileName = "_notification_logs." + (type.equalsIgnoreCase("CSV") ? "csv" : "xlsx");
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
