package com.middleware.backend.scheduledJobs.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.backend.scheduledJobs.DTO.JobExecutionDTO;
import com.middleware.backend.scheduledJobs.DTO.JobRequest;
import com.middleware.backend.scheduledJobs.enums.Status;
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
    private final JobLogsService logsService;


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

    public ResponseEntity<?> createNewJob(JobRequest job) {
        try {
            ScheduledJobs exists = repo.findByApiEndpointAndMethodAndEnabledTrue(
                    job.getApiEndpoint(),
                    job.getMethod()
            );

            if (exists != null) {
                return ResponseEntity
                        .badRequest()
                        .body("A job with the same API endpoint and method already exists.");
            }

            job.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            job.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            job.setActive(true);

            ScheduledJobs savedJob = repo.save(Mapper.mapToEntity(job));
            job.setId(savedJob.getId());

            // Schedule the job
            scheduleJob(job);

            // Log success
            JobExecutionDTO log = JobExecutionDTO.builder()
                    .scheduledJobName(savedJob.getJobName())
                    .scheduledJobMethod(savedJob.getMethod())
                    .scheduledJobPath(savedJob.getApiEndpoint())
                    .status(Status.SUCCESS)
                    .action("Create")
                    .createdAt(new Timestamp(System.currentTimeMillis()))
                    .build();
            logsService.save(log);

            return ResponseEntity.ok(savedJob);

        } catch (Exception e) {
            // Log failure
            JobExecutionDTO log = JobExecutionDTO.builder()
                    .scheduledJobName(job.getJobName())
                    .scheduledJobMethod(job.getMethod())
                    .scheduledJobPath(job.getApiEndpoint())
                    .status(Status.FAILURE)
                    .action("Create")
                    .errorMessage(e.getMessage())
                    .createdAt(new Timestamp(System.currentTimeMillis()))
                    .build();
            logsService.save(log);

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "fail",
                            "error", e.getMessage()
                    ));
        }
    }

    public ResponseEntity<?> pauseJob(Long id) {
        try {
            Optional<ScheduledJobs> exists = repo.findById(id);

            if (exists.isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body("Job was not found.");
            }

            ScheduledJobs job = exists.get();
            job.setEnabled(false);
            job.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            repo.save(job);

            scheduler.pauseJob(JobKey.jobKey(job.getJobName(), "http-jobs"));

            // Log success
            JobExecutionDTO log = JobExecutionDTO.builder()
                    .scheduledJobName(job.getJobName())
                    .scheduledJobMethod(job.getMethod())
                    .scheduledJobPath(job.getApiEndpoint())
                    .status(Status.SUCCESS)
                    .action("pause")
                    .createdAt(new Timestamp(System.currentTimeMillis()))
                    .build();
            logsService.save(log);

            return ResponseEntity.ok("Job paused successfully.");
        } catch (Exception e) {
            // Log failure
            JobExecutionDTO log = JobExecutionDTO.builder()
                    .scheduledJobName(id != null ? String.valueOf(id) : "Unknown Job ID")
                    .scheduledJobMethod(null)
                    .scheduledJobPath(null)
                    .status(Status.FAILURE)
                    .action("pause")
                    .errorMessage(e.getMessage())
                    .createdAt(new Timestamp(System.currentTimeMillis()))
                    .build();
            logsService.save(log);

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "fail",
                            "error", e.getMessage()
                    ));
        }
    }



    public ResponseEntity<?> resumeJob(Long id) {
        try {
            Optional<ScheduledJobs> exists = repo.findById(id);

            if (exists.isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body("Job was not found.");
            }

            ScheduledJobs job = exists.get();
            job.setEnabled(true);
            job.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            repo.save(job);

            JobKey jobKey = JobKey.jobKey(job.getJobName(), "http-jobs");
            TriggerKey triggerKey = TriggerKey.triggerKey(job.getJobName() + "-trigger", "http-triggers");

            if (!scheduler.checkExists(jobKey)) {
                scheduleJob(Mapper.mapToDTO(job));
            } else {
                scheduler.resumeJob(jobKey);
            }

            // Success log
            JobExecutionDTO log = JobExecutionDTO.builder()
                    .scheduledJobName(job.getJobName())
                    .scheduledJobMethod(job.getMethod())
                    .scheduledJobPath(job.getApiEndpoint())
                    .status(Status.SUCCESS)
                    .action("resume")
                    .createdAt(new Timestamp(System.currentTimeMillis()))
                    .build();
            logsService.save(log);

            return ResponseEntity.ok("Job resumed successfully.");
        } catch (Exception e) {
            // Failure log
            JobExecutionDTO log = JobExecutionDTO.builder()
                    .scheduledJobName(id != null ? String.valueOf(id) : "Unknown Job ID")
                    .scheduledJobMethod(null)
                    .scheduledJobPath(null)
                    .status(Status.FAILURE)
                    .action("resume")
                    .errorMessage(e.getMessage())
                    .createdAt(new Timestamp(System.currentTimeMillis()))
                    .build();
            logsService.save(log);

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "fail",
                            "error", e.getMessage()
                    ));
        }
    }



    public ResponseEntity<?> editJob(JobRequest job) {
        try {
            Optional<ScheduledJobs> exists = repo.findById(job.getId());

            if (exists.isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body("Job was not found.");
            }

            ScheduledJobs existingJob = exists.get();

            // Unschedule the existing job
            JobKey jobKey = JobKey.jobKey(existingJob.getJobName(), "http-jobs");
            TriggerKey triggerKey = TriggerKey.triggerKey(existingJob.getJobName() + "-trigger", "http-triggers");

            scheduler.pauseTrigger(triggerKey);
            scheduler.unscheduleJob(triggerKey);
            scheduler.deleteJob(jobKey);

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
                scheduleJob(Mapper.mapToDTO(existingJob));
            }

            // Success log
            JobExecutionDTO log = JobExecutionDTO.builder()
                    .scheduledJobName(existingJob.getJobName())
                    .scheduledJobMethod(existingJob.getMethod())
                    .scheduledJobPath(existingJob.getApiEndpoint())
                    .status(Status.SUCCESS)
                    .action("edit")
                    .createdAt(new Timestamp(System.currentTimeMillis()))
                    .build();
            logsService.save(log);

            return ResponseEntity.ok(updatedJob);

        } catch (Exception e) {
            // Failure log
            JobExecutionDTO log = JobExecutionDTO.builder()
                    .scheduledJobName(job.getJobName() != null ? job.getJobName() : "Unknown")
                    .scheduledJobMethod(job.getMethod())
                    .scheduledJobPath(job.getApiEndpoint())
                    .status(Status.FAILURE)
                    .action("edit")
                    .errorMessage(e.getMessage())
                    .createdAt(new Timestamp(System.currentTimeMillis()))
                    .build();
            logsService.save(log);

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "fail",
                            "error", e.getMessage()
                    ));
        }
    }



    public Page<ScheduledJobs> getAllRoutes(Specification<ScheduledJobs> spec, Pageable pageable) {
        Specification<ScheduledJobs> activeSpec = (root, query, cb) -> cb.isTrue(root.get("active"));
        return repo.findAll(spec.and(activeSpec), pageable);
    }

    public ResponseEntity<?> deactivateJob(Long id) {
        try {
            Optional<ScheduledJobs> jobOpt = repo.findById(id);

            if (jobOpt.isEmpty()) {
                JobExecutionDTO failLog = JobExecutionDTO.builder()
                        .scheduledJobName("Unknown")
                        .status(Status.FAILURE)
                        .action("deactivate")
                        .errorMessage("Job not found")
                        .createdAt(new Timestamp(System.currentTimeMillis()))
                        .build();
                logsService.save(failLog);

                return ResponseEntity
                        .badRequest()
                        .body("Job was not found.");
            }

            ScheduledJobs job = jobOpt.get();
            job.setActive(false);
            repo.save(job);

            // Pause job in scheduler
            pauseJob(id);

            JobExecutionDTO log = JobExecutionDTO.builder()
                    .scheduledJobName(job.getJobName())
                    .scheduledJobMethod(job.getMethod())
                    .scheduledJobPath(job.getApiEndpoint())
                    .status(Status.SUCCESS)
                    .action("deactivate")
                    .createdAt(new Timestamp(System.currentTimeMillis()))
                    .build();
            logsService.save(log);

            return ResponseEntity.ok(Mapper.mapToDTO(job));

        } catch (Exception e) {
            JobExecutionDTO failLog = JobExecutionDTO.builder()
                    .scheduledJobName("Unknown")
                    .status(Status.FAILURE)
                    .action("deactivate")
                    .errorMessage(e.getMessage())
                    .createdAt(new Timestamp(System.currentTimeMillis()))
                    .build();
            logsService.save(failLog);

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "fail",
                            "error", e.getMessage()
                    ));
        }
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


            JobExecutionDTO log = JobExecutionDTO.builder()
                    .scheduledJobName(job.getJobName())
                    .scheduledJobMethod(job.getMethod())
                    .scheduledJobPath(job.getApiEndpoint())
                    .status(Status.SUCCESS)
                    .action("test")
                    .createdAt(new Timestamp(System.currentTimeMillis()))
                    .build();
            logsService.save(log);

            boolean success = response.getStatusCode().is2xxSuccessful();
            return ResponseEntity.ok(Map.of("status", success ? "pass" : "fail"));

        } catch (Exception e) {
            JobExecutionDTO log = JobExecutionDTO.builder()
                    .scheduledJobName(job.getJobName())
                    .scheduledJobMethod(job.getMethod())
                    .scheduledJobPath(job.getApiEndpoint())
                    .status(Status.FAILURE)
                    .action("test")
                    .errorMessage(e.getMessage())
                    .createdAt(new Timestamp(System.currentTimeMillis()))
                    .build();
            logsService.save(log);
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
