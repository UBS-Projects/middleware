package com.middleware.backend.scheduledJobs.controller;


import com.middleware.backend.scheduledJobs.enums.Status;
import com.middleware.backend.scheduledJobs.model.ExecutionHistory;
import com.middleware.backend.scheduledJobs.service.JobLogsService;
import com.middleware.backend.scheduledJobs.specification.JobLogsSpecification;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
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

/**
 * REST controller exposing audit logs for user operations on scheduled jobs.
 */
@RestController
@RequestMapping("/api/scheduledjobslogs")
@AllArgsConstructor
public class JobLogsController {
    private final JobLogsService service;

        /**
         * Retrieves a paginated list of user actions performed on scheduled jobs.
         *
         * @param jobName optional job name filter (contains)
         * @param userEmail optional user email filter (contains)
         * @param apiEndpoint optional API endpoint filter (contains)
         * @param status optional action status filter (exact)
         * @param sortedBy field to sort by (default createdAt)
         * @param sortDirection asc/desc (default desc)
         * @param createdAfter optional lower bound for creation date (inclusive)
         * @param createdBefore optional upper bound for creation date (inclusive)
         * @param page zero-based page index
         * @param size page size
         * @return list of logs matching filters
         */
    @GetMapping()
    @PreAuthorize("hasAuthority('scheduledJobsLogs:view')")
    @Operation(
            summary = "List user operations on scheduled jobs",
            description = "Retrieves a paginated list of operations performed by users on scheduled jobs, " +
                    "including actions like create, pause, resume, update, and deactivate. " +
                    "Supports filtering by job name, user email, API endpoint, status, and date range. Requires 'scheduledJobsLogs:view' authority."
    )
    public ResponseEntity<Page<?>> getAll(
            @RequestParam(required = false) String jobName,
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) String apiEndpoint,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false, defaultValue = "createdAt") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdBefore,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size
    ){

        Pageable pageable = PageRequest.of(page, size, sortDirection.equalsIgnoreCase("asc")
                ? Sort.by(sortedBy).ascending()
                : Sort.by(sortedBy).descending());
        Specification<ExecutionHistory> spec = Specification
                .where(JobLogsSpecification.hasField("scheduledJobName", jobName, JobLogsSpecification.MatchMode.CONTAINS))
                .and(JobLogsSpecification.hasField("userEmail", userEmail, JobLogsSpecification.MatchMode.CONTAINS))
                .and(JobLogsSpecification.hasField("scheduledJobPath", apiEndpoint, JobLogsSpecification.MatchMode.CONTAINS))
                .and(JobLogsSpecification.hasField("status", status, JobLogsSpecification.MatchMode.EXACT))
                .and(JobLogsSpecification.createdBetween(createdBefore, createdAfter));
        return service.getAll(spec,pageable);

    }


    /**
     * Exports audit logs into CSV or Excel file.
     *
     * @param jobName optional job name filter (contains)
     * @param apiEndpoint optional API endpoint filter (contains)
     * @param status optional action status filter (exact)
     * @param sortedBy field to sort by (default createdAt)
     * @param sortDirection asc/desc (default desc)
     * @param createdAfter optional lower bound date (inclusive)
     * @param createdBefore optional upper bound date (inclusive)
     * @param type export type, CSV or EXCEL/XLSX
     * @return the exported file bytes with content headers
     */
    @GetMapping("/export/{type}")
    @PreAuthorize("hasAuthority('scheduledJobsLogs:export')")
    @Operation(
            summary = "Export user operations on scheduled jobs",
            description = "Exports the operations performed by users on scheduled jobs to CSV or Excel format. " +
                    "Supports filtering by job name, API endpoint, status, and date range. " +
                    "Includes details of actions such as create, edit, pause, resume, test, and deactivate. Requires 'scheduledJobsLogs:export' authority."
    )
    public ResponseEntity<byte[]> exportFile(
            @RequestParam(required = false) String jobName,
            @RequestParam(required = false) String apiEndpoint,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false, defaultValue = "createdAt") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdBefore,
            @PathVariable("type") String type
    ) {
        try {
            Specification<ExecutionHistory> spec = Specification
                    .where(JobLogsSpecification.hasField("scheduledJobName", jobName, JobLogsSpecification.MatchMode.CONTAINS))
                    .and(JobLogsSpecification.hasField("scheduledJobPath", apiEndpoint, JobLogsSpecification.MatchMode.CONTAINS))
                    .and(JobLogsSpecification.hasField("status", status, JobLogsSpecification.MatchMode.EXACT))
                    .and(JobLogsSpecification.createdBetween(createdBefore, createdAfter));

            byte[] fileBytes = service.exportFile(spec, type, sortedBy,sortDirection);

            if (fileBytes == null || fileBytes.length == 0) {
                return ResponseEntity.noContent().build();
            }

            String fileName = "job_logs." + (type.equalsIgnoreCase("CSV") ? "csv" : "xlsx");
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
