package com.middleware.backend.audit_logs_interceptor.controller;

import com.middleware.backend.audit_logs_interceptor.model.AuditResponse;
import com.middleware.backend.audit_logs_interceptor.service.AuditLogsService;
import com.middleware.backend.audit_logs_interceptor.specification.AuditLogSpecification;
import com.middleware.backend.audit_logs_interceptor.model.AuditLog;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for accessing and managing audit logs.
 * Provides endpoints for retrieving, filtering, and exporting audit log data.
 * Access to these endpoints is protected by method-level security.
 */
@RestController
@RequestMapping("/api/auditlogs")
@AllArgsConstructor
public class AuditLogsController {

    private final AuditLogsService service;

    /**
     * Retrieves a paginated list of audit logs based on specified filter criteria.
     * Supports filtering by user, method, API path, response status, and a date range.
     * Also supports sorting and pagination.
     *
     * @param user           Optional filter for the user associated with the log.
     * @param method         Optional filter for the HTTP method (e.g., GET, POST).
     * @param apiPath        Optional filter for the API path.
     * @param startTime      Optional start of the date range for filtering logs (format: yyyy-MM-dd HH:mm:ss).
     * @param endTime        Optional end of the date range for filtering logs (format: yyyy-MM-dd HH:mm:ss).
     * @param responseStatus Optional filter for the HTTP response status code.
     * @param sortedBy       The field to sort the results by. Defaults to "startTime".
     * @param sortDirection  The direction of the sort (asc or desc). Defaults to "desc".
     * @param page           The page number for pagination. Defaults to 0.
     * @param size           The number of items per page. Defaults to 10.
     * @return A {@link ResponseEntity} containing a {@link Page} of {@link AuditResponse} objects.
     */
    @GetMapping("")
    @PreAuthorize("hasAuthority('auditLogs:view')")
    @Operation(
            summary = "Get all audit logs",
            description = "Fetches a paginated list of audit logs with optional filters for user, method, API path, response status, and date range. Supports sorting and pagination. Requires 'auditLogs:view' authority."
    )
    public ResponseEntity<Page<AuditResponse>> getAll(
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String apiPath,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) Integer responseStatus,
            @RequestParam(required = false, defaultValue = "startTime") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Specification<AuditLog> spec = Specification
                    .where(AuditLogSpecification.hasField("userName", user, AuditLogSpecification.MatchMode.CONTAINS))
                    .and(AuditLogSpecification.hasField("method", method, AuditLogSpecification.MatchMode.EXACT))
                    .and(AuditLogSpecification.hasField("apiPath", apiPath, AuditLogSpecification.MatchMode.CONTAINS))
                    .and(AuditLogSpecification.hasField("responseStatus", responseStatus, AuditLogSpecification.MatchMode.EXACT))
                    .and(AuditLogSpecification.hasDateBetween("startTime", startTime, endTime));
            Pageable pageable = PageRequest.of(page, size,
                    sortDirection.equalsIgnoreCase("asc")
                            ? Sort.by(sortedBy).ascending()
                            : Sort.by(sortedBy).descending());


            Page<AuditResponse> result = service.getAll(spec,pageable);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Simple reusable filter builder


    /**
     * Retrieves a single audit log entry by its unique ID.
     *
     * @param id The unique identifier of the audit log to retrieve.
     * @return A {@link ResponseEntity} containing the audit log details or a not-found response.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('auditLogs:view')")
    @Operation(
            summary = "Get audit log by ID",
            description = "Fetches a specific audit log entry using its unique ID. Requires 'auditLogs:view' authority."
    )
    public ResponseEntity<?> getAll(
            @PathVariable("id") Long id) {
        return service.findById(id);
    }



//    ********************************************** Export File ***************************************************

    /**
     * Exports audit logs to a file (CSV or Excel) based on specified filter criteria.
     *
     * @param user           Optional filter for the user associated with the log.
     * @param method         Optional filter for the HTTP method.
     * @param apiPath        Optional filter for the API path.
     * @param startTime      Optional start of the date range for filtering.
     * @param endTime        Optional end of the date range for filtering.
     * @param responseStatus Optional filter for the HTTP response status code.
     * @param sortedBy       The field to sort the results by.
     * @param sortDirection  The direction of the sort.
     * @param type           The desired file format for export ("CSV" or "Excel").
     * @return A {@link ResponseEntity} containing the exported file as a byte array.
     */
    @GetMapping("/export/{type}")
    @PreAuthorize("hasAuthority('auditLogs:export')")
    @Operation(
            summary = "Export audit logs",
            description = "Exports audit logs in the requested format (CSV or Excel). Supports filters for user, method, API path, response status, and date range. Requires 'auditLogs:export' authority."
    )
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String apiPath,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) Integer responseStatus,
            @RequestParam(required = false, defaultValue = "startTime") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @PathVariable("type") String type
    ) {
        try {
            Specification<AuditLog> spec = Specification
                    .where(AuditLogSpecification.hasField("userName", user, AuditLogSpecification.MatchMode.CONTAINS))
                    .and(AuditLogSpecification.hasField("method", method, AuditLogSpecification.MatchMode.EXACT))
                    .and(AuditLogSpecification.hasField("apiPath", apiPath, AuditLogSpecification.MatchMode.CONTAINS))
                    .and(AuditLogSpecification.hasField("responseStatus", responseStatus, AuditLogSpecification.MatchMode.EXACT))
                    .and(AuditLogSpecification.hasDateBetween("startTime", startTime, endTime));
            Pageable pageable = PageRequest.of(0, 100000,
                    sortDirection.equalsIgnoreCase("asc")
                            ? Sort.by(sortedBy).ascending()
                            : Sort.by(sortedBy).descending());




            byte[] fileBytes = service.exportFile(spec, pageable, type);


            String fileName = "audit_logs." + (type.equalsIgnoreCase("CSV") ? "csv" : "xlsx");
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
