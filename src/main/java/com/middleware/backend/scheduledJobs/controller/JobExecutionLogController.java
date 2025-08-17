package com.middleware.backend.scheduledJobs.controller;

import com.middleware.backend.scheduledJobs.enums.Status;
import com.middleware.backend.scheduledJobs.model.ExecutionHistory;
import com.middleware.backend.scheduledJobs.model.JobExecutionLogs;
import com.middleware.backend.scheduledJobs.service.JobExecutionlogsService;
import com.middleware.backend.scheduledJobs.specification.JobExecutionLogsSpecification;
import com.middleware.backend.scheduledJobs.specification.JobLogsSpecification;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/executionlogs")
@AllArgsConstructor
public class JobExecutionLogController {
    private final JobExecutionlogsService service;
    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) String jobName,
            @RequestParam(required = false) String apiEndpoint,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false, defaultValue = "startTime") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                sortDirection.equalsIgnoreCase("asc") ? Sort.by(sortedBy).ascending() : Sort.by(sortedBy).descending()
        );

        Specification<JobExecutionLogs> spec = Specification
                .where(JobExecutionLogsSpecification.hasField("job.jobName", jobName, JobExecutionLogsSpecification.MatchMode.CONTAINS))
                .and(JobExecutionLogsSpecification.hasField("job.apiEndpoint", apiEndpoint, JobExecutionLogsSpecification.MatchMode.CONTAINS))
                .and(JobExecutionLogsSpecification.hasField("status", status, JobExecutionLogsSpecification.MatchMode.EXACT))
                .and(JobExecutionLogsSpecification.startedBetween(startAfter, startBefore));

        return service.getAll(spec, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(
            @PathVariable("id") Long id
    ){
        return service.getById(id);
    }




    @GetMapping("/export/{type}")
    public ResponseEntity<byte[]> exportFile(
            @RequestParam(required = false) String jobName,
            @RequestParam(required = false) String apiEndpoint,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false, defaultValue = "startTime") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startBefore,
            @PathVariable("type") String type
    ) {
        try {
        Pageable pageable = PageRequest.of(
                0,
                100000,
                sortDirection.equalsIgnoreCase("asc") ? Sort.by(sortedBy).ascending() : Sort.by(sortedBy).descending()
        );

        Specification<JobExecutionLogs> spec = Specification
                .where(JobExecutionLogsSpecification.hasField("job.jobName", jobName, JobExecutionLogsSpecification.MatchMode.CONTAINS))
                .and(JobExecutionLogsSpecification.hasField("job.apiEndpoint", apiEndpoint, JobExecutionLogsSpecification.MatchMode.CONTAINS))
                .and(JobExecutionLogsSpecification.hasField("status", status, JobExecutionLogsSpecification.MatchMode.EXACT))
                .and(JobExecutionLogsSpecification.startedBetween(startAfter, startBefore));


        byte[] fileBytes = service.exportFile(spec, pageable, type);

        String fileName = "_scheduled_execution_logs." + (type.equalsIgnoreCase("CSV") ? "csv" : "xlsx");
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
