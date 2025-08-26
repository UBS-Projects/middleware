package com.middleware.backend.scheduledJobs.controller;

import com.middleware.backend.scheduledJobs.DTO.JobExecutionDTO;
import com.middleware.backend.scheduledJobs.DTO.JobRequest;
import com.middleware.backend.scheduledJobs.enums.MatchMode;
import com.middleware.backend.scheduledJobs.enums.Status;
import com.middleware.backend.scheduledJobs.model.ScheduledJobs;
import com.middleware.backend.scheduledJobs.service.JobLogsService;
import com.middleware.backend.scheduledJobs.service.ScheduledJobsService;
import com.middleware.backend.scheduledJobs.specification.ScheduledJobsSpecification;
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

@RestController
@RequestMapping("/scheduledjobs")
@AllArgsConstructor
public class ScheduledJobsController {
    private final ScheduledJobsService service;
    private final JobLogsService logsService;

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

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('scheduledJobs:pause','scheduledJobs:resume')")
    public ResponseEntity<?> changeStatus(@PathVariable Long id,
                                          @RequestParam("status") String status) throws SchedulerException {
        String action = status.equals("pause") ? "pause" : "resume";
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("Authenticated user email: " + email);

        try {
            ScheduledJobs job = status.equals("pause") ? service.pauseJob(id,email) : service.resumeJob(id, email);
            logJob(job.getJobName(), job.getMethod(), job.getApiEndpoint(), action, true, email, null);
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            logJob("Job-" + id, null, null, action, false, email, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("")
    @PreAuthorize("hasAuthority('scheduledJobs:edit')")
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


    @PostMapping()
    @PreAuthorize("hasAuthority('scheduledJobs:create')")
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

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('scheduledJobs:view')")
    public ResponseEntity<?> getById(@PathVariable long id){
        return service.getById(id);
    }

    @GetMapping("")
    @PreAuthorize("hasAuthority('scheduledJobs:view')")
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
            System.out.println("kkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkk");
            System.out.println("Authenticated user email: " + email);
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('scheduledJobs:delete')")
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

    @PostMapping("/test")
    @PreAuthorize("hasAuthority('scheduledJobs:test')")
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




    @GetMapping("/export/{type}")
    @PreAuthorize("hasAuthority('scheduledJobs:export')")
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