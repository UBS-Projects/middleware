package com.middleware.backend.scheduledJobs.controller;

import com.middleware.backend.scheduledJobs.DTO.JobExecutionDTO;
import com.middleware.backend.scheduledJobs.DTO.JobRequest;
import com.middleware.backend.scheduledJobs.enums.MatchMode;
import com.middleware.backend.scheduledJobs.enums.Status;
import com.middleware.backend.scheduledJobs.model.ScheduledJobs;
import com.middleware.backend.scheduledJobs.service.JobLogsService;
import com.middleware.backend.scheduledJobs.service.ScheduledJobsService;
import com.middleware.backend.scheduledJobs.specification.ScheduledJobsSpecification;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.quartz.SchedulerException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.Map;

/**
 * REST controller for managing scheduled jobs lifecycle and querying definitions.
 * <p>
 * Supports create, edit, pause, resume, deactivate, list, get by id, test, and export actions.
 */
@RestController
@RequestMapping("/api/scheduledjobs")
@AllArgsConstructor
public class ScheduledJobsController {
    private final ScheduledJobsService service;
    private final JobLogsService logsService;

    /**
     * Utility to persist an audit log for a job-related action.
     */
    private void logJob(String jobName, String method, String path, String action, boolean success, String email, String error) {
        JobExecutionDTO log = JobExecutionDTO.builder()
                .scheduledJobName(jobName)
                .scheduledJobMethod(method)
                .scheduledJobPath(path)
                .action(action)
                .status(success ? Status.SUCCESS : Status.FAILURE)
                .errorMessage(error)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .userEmail(email)
                .build();
        logsService.save(log);
    }

    /**
     * Pauses or resumes a scheduled job by id.
     *
     * @param id job id to change status for
     * @param status either "pause" or anything else treated as resume
     * @return the updated job definition or an error response
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('scheduledJobs:pause','scheduledJobs:resume')")
    @Operation(
            summary = "Pause or resume a scheduled job",
            description = "Allows pausing or resuming a scheduled job by its ID. Requires appropriate permissions. Requires either 'scheduledJobs:pause' or 'scheduledJobs:resume' authority."
    )
    public ResponseEntity<?> changeStatus(@PathVariable Long id,
                                          @RequestParam("status") String status) throws SchedulerException {
        String action = status.equals("pause") ? "pause" : "resume";
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        try {
            ScheduledJobs job = status.equals("pause") ? service.pauseJob(id,email) : service.resumeJob(id, email);
            logJob(job.getJobName(), job.getMethod(), job.getApiEndpoint(), action, true, email, null);
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            logJob("Job-" + id, null, null, action, false, email, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Edits an existing job definition.
     *
     * @param job the new job request payload
     * @return the updated job if successful or error response
     */
    @PatchMapping("")
    @PreAuthorize("hasAuthority('scheduledJobs:edit')")
    @Operation(
            summary = "Edit a scheduled job",
            description = "Updates an existing scheduled job definition with new details. Requires 'scheduledJobs:edit' authority."
    )
    public ResponseEntity<?> edit(@RequestBody JobRequest job) throws SchedulerException {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            ScheduledJobs updatedJob = service.editJob(job, email);
            logJob(updatedJob.getJobName(), updatedJob.getMethod(), updatedJob.getApiEndpoint(), "edit", true, email, null);
            return ResponseEntity.ok(updatedJob);
        } catch (Exception e) {
            logJob(job.getJobName(), job.getMethod(), job.getApiEndpoint(), "edit", false, email, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }


    /**
     * Creates and schedules a new job definition.
     *
     * @param job the job request payload
     * @return the created job or error response
     */
    @PostMapping()
    @PreAuthorize("hasAuthority('scheduledJobs:create')")
    @Operation(
            summary = "Create a new scheduled job",
            description = "Creates and schedules a new job based on the provided job request. Requires 'scheduledJobs:create' authority."
    )
    public ResponseEntity<?> createJob(@RequestBody JobRequest job) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            ScheduledJobs savedJob = service.createNewJob(job,email);
            logJob(savedJob.getJobName(), savedJob.getMethod(), savedJob.getApiEndpoint(), "create", true, email, null);
            return ResponseEntity.ok(savedJob);
        } catch (Exception e) {
            logJob(job.getJobName(), job.getMethod(), job.getApiEndpoint(), "create", false, email, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Retrieves a job definition by id.
     *
     * @param id the job id
     * @return the job details or not found/error from service
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('scheduledJobs:view')")
    @Operation(
            summary = "Get scheduled job by ID",
            description = "Fetches details of a scheduled job by its unique identifier. Requires 'scheduledJobs:view' authority."
    )
    public ResponseEntity<?> getById(@PathVariable long id){
        return service.getById(id);
    }

    /**
     * Retrieves a paginated list of scheduled jobs with optional filters.
     *
     * @param jobName optional contains filter by job name
     * @param apiEndpoint optional contains filter by API endpoint
     * @param method optional exact match filter by HTTP method
     * @param enabled optional exact match filter for enabled flag
     * @param sortedBy field to sort by (default updatedAt)
     * @param sortDirection asc/desc (default desc)
     * @param page zero-based page index
     * @param size page size
     * @return page of scheduled jobs or error response
     */
    @GetMapping("")
    @PreAuthorize("hasAuthority('scheduledJobs:view')")
    @Operation(
            summary = "List scheduled jobs",
            description = "Retrieves a paginated list of scheduled jobs with optional filters for job name, API endpoint, HTTP method, and enabled status. Requires 'scheduledJobs:view' authority."
    )
    public ResponseEntity<Page<?>> getAll(
            @RequestParam(required = false) String jobName,
            @RequestParam(required = false) String apiEndpoint,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false, defaultValue = "updatedAt") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

        try {
            Specification<ScheduledJobs> spec = Specification
                    .where(ScheduledJobsSpecification.hasField("jobName", jobName, MatchMode.CONTAINS))
                    .and(ScheduledJobsSpecification.hasField("apiEndpoint", apiEndpoint, MatchMode.CONTAINS))
                    .and(ScheduledJobsSpecification.hasField("method", method, MatchMode.EXACT))
                    .and(ScheduledJobsSpecification.hasField("enabled", enabled));
            Pageable pageable = PageRequest.of(page, size, sortDirection.equalsIgnoreCase("asc")
                    ? Sort.by(sortedBy).ascending()
                    : Sort.by(sortedBy).descending());
            Page<?> result = service.getAllRoutes(spec, pageable);
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }


    /**
     * Deactivates (soft deletes) a job by id.
     *
     * @param id the job id
     * @return the deactivated job or error response
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('scheduledJobs:delete')")
    @Operation(
            summary = "Deactivate a scheduled job",
            description = "Deactivates (soft deletes) a scheduled job by its ID. Requires 'scheduledJobs:delete' authority."
    )
    public ResponseEntity<?> deactivateJob(@PathVariable Long id) throws SchedulerException {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            ScheduledJobs job = service.deactivateJob(id, email);
            logJob(job.getJobName(), job.getMethod(), job.getApiEndpoint(), "deactivate", true, email, null);
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            logJob("Job-" + id, null, null, "deactivate", false, email, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Tests a job configuration without persisting changes.
     *
     * @param jobRequest the job to test
     * @return status pass/fail depending on test outcome
     */
    @PostMapping("/test")
    @PreAuthorize("hasAuthority('scheduledJobs:test')")
    @Operation(
            summary = "Test a scheduled job",
            description = "Tests a scheduled job configuration without deploying it, returning pass or fail status. Requires 'scheduledJobs:test' authority."
    )
    public ResponseEntity<?> test(@RequestBody JobRequest jobRequest) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            boolean success = service.test(jobRequest);
            logJob(jobRequest.getJobName(), jobRequest.getMethod(), jobRequest.getApiEndpoint(), "test", success, email, null);
            return ResponseEntity.ok(Map.of("status", success ? "pass" : "fail"));
        } catch (Exception e) {
            logJob(jobRequest.getJobName(), jobRequest.getMethod(), jobRequest.getApiEndpoint(), "test", false, email, e.getMessage());
            return ResponseEntity.ok(Map.of("status", "fail"));
        }
    }




    /**
     * Exports scheduled jobs to CSV or Excel.
     *
     * @param jobName optional contains filter by job name
     * @param apiEndpoint optional contains filter by API endpoint
     * @param method optional exact match filter by HTTP method
     * @param enabled optional exact match filter by enabled flag
     * @param sortedBy field to sort by (default updatedAt)
     * @param sortDirection asc/desc (default desc)
     * @param type export type CSV or EXCEL/XLSX
     * @return the exported file bytes with appropriate headers
     */
    @GetMapping("/export/{type}")
    @PreAuthorize("hasAuthority('scheduledJobs:export')")
    @Operation(
            summary = "Export scheduled jobs",
            description = "Exports scheduled jobs to CSV or Excel format with optional filters for job name, API endpoint, method, and enabled status. Requires 'scheduledJobs:export' authority."
    )
    public ResponseEntity<byte[]> exportFile(
            @RequestParam(required = false) String jobName,
            @RequestParam(required = false) String apiEndpoint,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false, defaultValue = "updatedAt") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @PathVariable("type") String type) {

        try {
            Specification<ScheduledJobs> spec = Specification
                    .where(ScheduledJobsSpecification.hasField("jobName", jobName, MatchMode.CONTAINS))
                    .and(ScheduledJobsSpecification.hasField("apiEndpoint", apiEndpoint, MatchMode.CONTAINS))
                    .and(ScheduledJobsSpecification.hasField("method", method, MatchMode.EXACT))
                    .and(ScheduledJobsSpecification.hasField("enabled", enabled));
            Pageable pageable = PageRequest.of(0, 100000, sortDirection.equalsIgnoreCase("asc")
                    ? Sort.by(sortedBy).ascending()
                    : Sort.by(sortedBy).descending());

            byte[] fileBytes = service.exportFile(spec, pageable, type);

            String fileName = "scheduled_jobs." + (type.equalsIgnoreCase("CSV") ? "csv" : "xlsx");
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