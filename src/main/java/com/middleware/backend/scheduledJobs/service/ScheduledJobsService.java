package com.middleware.backend.scheduledJobs.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.backend.scheduledJobs.DTO.JobRequest;
import com.middleware.backend.scheduledJobs.mapper.Mapper;
import com.middleware.backend.scheduledJobs.model.ScheduledJobs;
import com.middleware.backend.scheduledJobs.repository.ScheduledJobRepository;
import lombok.AllArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.quartz.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;


import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@AllArgsConstructor
public class ScheduledJobsService{
    private final ScheduledJobRepository repo;
    private Scheduler scheduler;
    private final RestTemplate restTemplate;


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
        ScheduledJobs exists = repo.findByApiEndpointAndMethodAndEnabledTrue(job.getApiEndpoint(), job.getMethod());

        if (exists != null) {
            return ResponseEntity
                    .badRequest()
                    .body("A job with the same API endpoint and method already exists.");
        }
        job.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        job.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        job.setActive(true);
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


    public ResponseEntity<?> resumeJob(Long id) throws SchedulerException {
        Optional<ScheduledJobs> exists = repo.findById(id);

        if (exists.isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body("Job was not FOUND.");
        }

        ScheduledJobs job = exists.get();
        job.setEnabled(true);
        job.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        repo.save(job);

        JobKey jobKey = JobKey.jobKey(job.getJobName(), "http-jobs");
        TriggerKey triggerKey = TriggerKey.triggerKey(job.getJobName() + "-trigger", "http-triggers");

        // Check if job already exists in the scheduler
        if (!scheduler.checkExists(jobKey)) {
            // If not scheduled, schedule it again
            scheduleJob(Mapper.mapToDTO(job));
        } else {
            // If scheduled but paused, just resume it
            scheduler.resumeJob(jobKey);
        }

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


    public Page<ScheduledJobs> getAllRoutes(Specification<ScheduledJobs> spec, Pageable pageable) {
        Specification<ScheduledJobs> activeSpec = (root, query, cb) -> cb.isTrue(root.get("active"));
        return repo.findAll(spec.and(activeSpec), pageable);
    }

    public ResponseEntity<?> deactivateJob(Long id) throws SchedulerException {
        Optional<ScheduledJobs> job = repo.findById(id);
        if(job.isPresent()){
            job.get().setActive(false);
            repo.save(job.get());
            pauseJob(id);
            return ResponseEntity.ok(Mapper.mapToDTO(job.get()));
        }
        return ResponseEntity.badRequest().body("an Error Occurred");
    }

    public ResponseEntity<?> test(JobRequest job) {
        try {
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> headersMap = objectMapper.readValue(job.getHeaders(), new TypeReference<>() {});
            headersMap.forEach(headers::add);

            // Prepare body
            HttpEntity<String> entity = new HttpEntity<>(job.getPayload(), headers);

            // Make request
            ResponseEntity<String> response = restTemplate.exchange(
                    job.getApiEndpoint(),
                    HttpMethod.valueOf(job.getMethod().toUpperCase()),
                    entity,
                    String.class
            );

            boolean success = response.getStatusCode().is2xxSuccessful();
            return ResponseEntity.ok(Map.of("status", success ? "pass" : "fail"));

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("status", "fail"));
        }
    }

    public byte[] exportFile(Specification<ScheduledJobs> spec, Pageable pageable, String type) throws IOException {
        Specification<ScheduledJobs> activeSpec = (root, query, cb) -> cb.isTrue(root.get("active"));
        Page<ScheduledJobs> res = repo.findAll(spec.and(activeSpec), pageable);
        List<ScheduledJobs> data = res.getContent();

        if ("CSV".equalsIgnoreCase(type)) {
            return convertToCSV(data).getBytes(StandardCharsets.UTF_8);
        } else if ("Excel".equalsIgnoreCase(type) || "XLSX".equalsIgnoreCase(type)) {
            return convertToExcel(data);
        } else {
            throw new IllegalArgumentException("Unsupported export type: " + type);
        }
    }

    // Helper method to convert List<ScheduledJobs> to CSV String
    private String convertToCSV(List<ScheduledJobs> jobs) {
        StringBuilder sb = new StringBuilder();
        // Add CSV headers
        sb.append("JobName,ApiEndpoint,Method,Enabled,Active,UpdatedAt\n");

        for (ScheduledJobs job : jobs) {
            sb.append(escapeCsv(job.getJobName())).append(",");
            sb.append(escapeCsv(job.getApiEndpoint())).append(",");
            sb.append(escapeCsv(job.getMethod())).append(",");
            sb.append(job.isEnabled()).append(",");
            sb.append(job.isActive()).append(",");
            sb.append(job.getUpdatedAt()).append("\n");
        }

        return sb.toString();
    }

    // Basic CSV escaping for commas, quotes
    private String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    // Helper method to convert List<ScheduledJobs> to Excel bytes
    private byte[] convertToExcel(List<ScheduledJobs> jobs) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Scheduled Jobs");

            // Create header row
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("JobName");
            header.createCell(1).setCellValue("ApiEndpoint");
            header.createCell(2).setCellValue("Method");
            header.createCell(3).setCellValue("Enabled");
            header.createCell(4).setCellValue("Active");
            header.createCell(5).setCellValue("UpdatedAt");

            // Fill data rows
            int rowIdx = 1;
            for (ScheduledJobs job : jobs) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(job.getJobName());
                row.createCell(1).setCellValue(job.getApiEndpoint());
                row.createCell(2).setCellValue(job.getMethod());
                row.createCell(3).setCellValue(job.isEnabled());
                row.createCell(4).setCellValue(job.isActive());
                if (job.getUpdatedAt() != null)
                    row.createCell(5).setCellValue(job.getUpdatedAt().toString());
            }

            // Autosize columns for better readability
            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write workbook to byte array
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                workbook.write(bos);
                return bos.toByteArray();
            }
        }
    }
}
