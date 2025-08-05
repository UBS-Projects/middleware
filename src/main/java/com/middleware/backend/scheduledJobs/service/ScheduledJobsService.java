package com.middleware.backend.scheduledJobs.service;

import com.middleware.backend.kaotocamel.model.DynamicRouteEntity;
import com.middleware.backend.scheduledJobs.DTO.JobRequest;
import com.middleware.backend.scheduledJobs.mapper.Mapper;
import com.middleware.backend.scheduledJobs.model.ScheduledJobs;
import com.middleware.backend.scheduledJobs.repository.ScheduledJobRepository;
import lombok.AllArgsConstructor;
import org.quartz.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ScheduledJobsService{
    private final ScheduledJobRepository repo;
    private Scheduler scheduler;

    public void scheduleAllActiveJobs() {
        List<ScheduledJobs> jobs = repo.findByEnabledTrue();
        List<JobRequest> jobDTOs = jobs.stream()
                .map(Mapper::mapToDTO)
                .toList();
        jobDTOs.forEach(this::scheduleJob);
    }

    private void scheduleJob(JobRequest job) {
        try {
            JobDetail jobDetail = JobBuilder.newJob(JobExecution.class)
                    .withIdentity(job.getJobName(), "http-jobs")
                    .usingJobData("url", job.getApiEndpoint())
                    .usingJobData("method", job.getMethod())
                    .usingJobData("headers", job.getHeaders())
                    .usingJobData("payload", job.getPayload())
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(job.getJobName() + "-trigger", "http-triggers")
                    .withSchedule(CronScheduleBuilder.cronSchedule(job.getScheduleExpression()))
                    .build();
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            throw new RuntimeException("Error scheduling job", e);
        }
    }

    public ResponseEntity<?> createNewJob(JobRequest job){
        ScheduledJobs exists = repo.findByApiEndpointAndMethod(job.getApiEndpoint(), job.getMethod());

        if (exists != null) {
            return ResponseEntity
                    .badRequest()
                    .body("A job with the same API endpoint and method already exists.");
        }
        job.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        job.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        ScheduledJobs savedJob = repo.save(Mapper.mapToEntity(job));
        scheduleJob(job);

        return ResponseEntity.ok(savedJob);
    }

    public ResponseEntity<?> pauseJob(Long id) throws SchedulerException {
        Optional<ScheduledJobs> exists = repo.findById(id);

        if (exists.isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body("job was not FOUND.");
        }
        exists.get().setEnabled(false);
        exists.get().setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        repo.save(exists.get());
        scheduler.pauseJob(JobKey.jobKey(exists.get().getJobName(), "http-jobs"));
        return ResponseEntity.ok("Job paused successfully.");
    }


    public ResponseEntity<?> resumeJob (Long id) throws SchedulerException {
        Optional<ScheduledJobs> exists = repo.findById(id);

        if (exists.isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body("job was not FOUND.");
        }
        exists.get().setEnabled(true);
        exists.get().setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        repo.save(exists.get());
        scheduler.resumeJob(JobKey.jobKey(exists.get().getJobName(), "http-jobs"));
        return ResponseEntity.ok("Job Resumed successfully.");
    }

    public ResponseEntity<?> editJob(JobRequest job) {
        Optional<ScheduledJobs> exists = repo.findById(job.getId());

        if (exists.isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body("Job was not found.");
        }

        ScheduledJobs existingJob = exists.get();

        // Unschedule the existing job
        try {
            JobKey jobKey = JobKey.jobKey(existingJob.getJobName(), "http-jobs");
            TriggerKey triggerKey = TriggerKey.triggerKey(existingJob.getJobName() + "-trigger", "http-triggers");
            scheduler.pauseTrigger(triggerKey);
            scheduler.unscheduleJob(triggerKey);
            scheduler.deleteJob(jobKey);
        } catch (SchedulerException e) {
            return ResponseEntity
                    .internalServerError()
                    .body("Failed to remove the old scheduled job: " + e.getMessage());
        }

        // Update fields
        if (job.getJobName() != null) existingJob.setJobName(job.getJobName());
        if (job.getDescription() != null) existingJob.setDescription(job.getDescription());
        if (job.getScheduleExpression() != null) existingJob.setScheduleExpression(job.getScheduleExpression());
        if (job.getApiEndpoint() != null) existingJob.setApiEndpoint(job.getApiEndpoint());
        if (job.getMethod() != null) existingJob.setMethod(job.getMethod());
        if (job.getHeaders() != null) existingJob.setHeaders(job.getHeaders());
        if (job.getPayload() != null) existingJob.setPayload(job.getPayload());
        if (job.getUpdatedBy() != null) existingJob.setUpdatedBy(job.getUpdatedBy());
        existingJob.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        existingJob.setEnabled(true);

        // Save updated job to DB
        ScheduledJobs updatedJob = repo.save(existingJob);

        // Reschedule if enabled
        if (updatedJob.isEnabled()) {
            try {
                scheduleJob(Mapper.mapToDTO(existingJob));
            } catch (Exception e) {
                return ResponseEntity
                        .internalServerError()
                        .body("Job was updated but failed to reschedule: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(updatedJob);
    }


    public ResponseEntity<?> getAllJobs(Pageable pageable) {
        return ResponseEntity.ok(repo.findAll(pageable));
    }

    public Page<ScheduledJobs> getAllRoutes(Specification<ScheduledJobs> spec, Pageable pageable) {
        return repo.findAll(spec, pageable);
    }
}
