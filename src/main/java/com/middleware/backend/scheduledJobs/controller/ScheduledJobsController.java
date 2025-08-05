package com.middleware.backend.scheduledJobs.controller;

import com.middleware.backend.kaotocamel.model.DynamicRouteEntity;
import com.middleware.backend.kaotocamel.spec.DynamicRouteSpecification;
import com.middleware.backend.scheduledJobs.DTO.JobRequest;
import com.middleware.backend.scheduledJobs.enums.MatchMode;
import com.middleware.backend.scheduledJobs.enums.ScheduleType;
import com.middleware.backend.scheduledJobs.model.ScheduledJobs;
import com.middleware.backend.scheduledJobs.repository.ScheduledJobRepository;
import com.middleware.backend.scheduledJobs.service.ScheduledJobsService;
import com.middleware.backend.scheduledJobs.specification.ScheduledJobsSpecification;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.quartz.SchedulerException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/scheduledjobs")
@AllArgsConstructor
public class ScheduledJobsController {
    private final ScheduledJobsService service;

    @PatchMapping("/{id}")
    public ResponseEntity<?> changeStatus(@PathVariable Long id,
                                          @RequestParam("status") String status) throws SchedulerException {
        if(status.equals("pause"))
            return service.pauseJob(id);
        else
            return service.resumeJob(id);
    }
    @PatchMapping("")
    public ResponseEntity<?> edit(@RequestBody JobRequest job) throws SchedulerException {
            return service.editJob(job);
    }

    @GetMapping()
    public ResponseEntity<?> getAllJobs(@RequestParam(value = "pagenum",defaultValue = "0") int pageNum, @RequestParam(value = "pagesize", defaultValue = "10") int pageSize){
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by("updatedAt").descending());
        return service.getAllJobs(pageable);
    }
    @PostMapping()
    public ResponseEntity<?> CreateJob(@RequestBody JobRequest job){
        return service.createNewJob(job);
    }
    @GetMapping("/search")
    public ResponseEntity<Page<?>> search(
            @RequestParam(required = false) String jobName,
            @RequestParam(required = false) String apiEndpoint,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) Boolean  enabled,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

        try {
            Specification<ScheduledJobs> spec = Specification
                    .where(ScheduledJobsSpecification.hasField("jobName", jobName, MatchMode.CONTAINS))
                    .and(ScheduledJobsSpecification.hasField("apiEndpoint", apiEndpoint, MatchMode.CONTAINS))
                    .and(ScheduledJobsSpecification.hasField("method", method, MatchMode.EXACT))
                    .and(ScheduledJobsSpecification.hasField("enabled", enabled));
            Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
            Page<?> result = service.getAllRoutes(spec, pageable);
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }


}
