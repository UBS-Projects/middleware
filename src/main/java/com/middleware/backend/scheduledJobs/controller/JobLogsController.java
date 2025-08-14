package com.middleware.backend.scheduledJobs.controller;


import com.middleware.backend.scheduledJobs.enums.Status;
import com.middleware.backend.scheduledJobs.model.ExecutionHistory;
import com.middleware.backend.scheduledJobs.service.JobLogsService;
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
@RequestMapping("/scheduledjobslogs")
@AllArgsConstructor
public class JobLogsController {
    private final JobLogsService service;

    @GetMapping()
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) String jobName,
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
                .and(JobLogsSpecification.hasField("scheduledJobPath", apiEndpoint, JobLogsSpecification.MatchMode.CONTAINS))
                .and(JobLogsSpecification.hasField("status", status, JobLogsSpecification.MatchMode.EXACT))
                .and(JobLogsSpecification.createdBetween(createdBefore, createdAfter));
        return service.getAll(spec,pageable);

    }


    @GetMapping("/export/{type}")
    public ResponseEntity<byte[]> exportFile(
            @RequestParam(required = false) String jobName,
            @RequestParam(required = false) String apiEndpoint,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false, defaultValue = "createdAt") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdBefore,
            @PathVariable("type") String type
            ){
        try {
        Pageable pageable = PageRequest.of(0, 100000, sortDirection.equalsIgnoreCase("asc")
                ? Sort.by(sortedBy).ascending()
                : Sort.by(sortedBy).descending());
        Specification<ExecutionHistory> spec = Specification
                .where(JobLogsSpecification.hasField("scheduledJobName", jobName, JobLogsSpecification.MatchMode.CONTAINS))
                .and(JobLogsSpecification.hasField("scheduledJobPath", apiEndpoint, JobLogsSpecification.MatchMode.CONTAINS))
                .and(JobLogsSpecification.hasField("status", status, JobLogsSpecification.MatchMode.EXACT))
                .and(JobLogsSpecification.createdBetween(createdBefore, createdAfter));
        byte[] fileBytes = service.exportFile(spec, pageable, type);

        String fileName = "_logs." + (type.equalsIgnoreCase("CSV") ? "csv" : "xlsx");
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
