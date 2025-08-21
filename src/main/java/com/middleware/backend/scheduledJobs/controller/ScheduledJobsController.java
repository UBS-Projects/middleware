package com.middleware.backend.scheduledJobs.controller;

import com.middleware.backend.scheduledJobs.DTO.JobRequest;
import com.middleware.backend.scheduledJobs.enums.MatchMode;
import com.middleware.backend.scheduledJobs.model.ScheduledJobs;
import com.middleware.backend.scheduledJobs.service.ScheduledJobsService;
import com.middleware.backend.scheduledJobs.specification.ScheduledJobsSpecification;
import lombok.AllArgsConstructor;
import org.quartz.SchedulerException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/scheduledjobs")
@AllArgsConstructor
public class ScheduledJobsController {
    private final ScheduledJobsService service;

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('scheduledJobs:pause','scheduledJobs:resume')")
    public ResponseEntity<?> changeStatus(@PathVariable Long id,
                                          @RequestParam("status") String status) throws SchedulerException {
        if (status.equals("pause"))
            return service.pauseJob(id);
        else
            return service.resumeJob(id);
    }

    @PatchMapping("")
    @PreAuthorize("hasAuthority('scheduledJobs:edit')")
    public ResponseEntity<?> edit(@RequestBody JobRequest job) throws SchedulerException {
        return service.editJob(job);
    }


    @PostMapping()
    @PreAuthorize("hasAuthority('scheduledJobs:create')")
    public ResponseEntity<?> CreateJob(@RequestBody JobRequest job) {
        return service.createNewJob(job);
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
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('scheduledJobs:delete')")
    public ResponseEntity<?> deactivateJob(@PathVariable Long id) throws SchedulerException {
        return service.deactivateJob(id);
    }

    @PostMapping("/test")
    @PreAuthorize("hasAuthority('scheduledJobs:test')")
    public ResponseEntity<?> test(@RequestBody JobRequest jobRequest) {
        return service.test(jobRequest);
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