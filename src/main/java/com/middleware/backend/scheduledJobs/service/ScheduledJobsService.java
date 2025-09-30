package com.middleware.backend.scheduledJobs.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.backend.scheduledJobs.DTO.JobExecutionDTO;
import com.middleware.backend.scheduledJobs.DTO.JobRequest;
import com.middleware.backend.scheduledJobs.enums.Status;
import com.middleware.backend.scheduledJobs.mapper.Mapper;
import com.middleware.backend.scheduledJobs.model.ScheduledJobs;
import com.middleware.backend.scheduledJobs.model.SingleJobDto;
import com.middleware.backend.scheduledJobs.repository.ScheduledJobRepository;
import com.middleware.backend.logging.repository.MiddlewareApiCallLogRepository;
import com.middleware.backend.users.model.User;
import com.middleware.backend.users.repository.UserRepository;
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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Service orchestrating lifecycle of scheduled jobs: create, edit, pause/resume, deactivate, test, list, and export.
 * Integrates with Quartz to schedule jobs and with repositories for persistence.
 */
@Service
@AllArgsConstructor
public class ScheduledJobsService{
    private final ScheduledJobRepository repo;
    private Scheduler scheduler;
    private final RestTemplate restTemplate;
    private final UserRepository userRepo;
    private final MiddlewareApiCallLogRepository middlewareApiCallLogRepository; // Add this dependency

    /**
     * Loads all enabled jobs from DB and schedules them with Quartz.
     */
    public void scheduleAllActiveJobs() {
        List<ScheduledJobs> jobs = repo.findByEnabledTrue();
        List<JobRequest> jobDTOs = jobs.stream()
                .map(Mapper::mapToDTO)
                .toList();
        jobDTOs.forEach(this::scheduleJob);
    }

    /**
     * Creates Quartz job + trigger for the given job definition.
     */
    private void scheduleJob(JobRequest job) {
        try {
            JobBuilder jobBuilder = JobBuilder.newJob(JobExecution.class)
                    .withIdentity(job.getJobName(), "http-jobs")
                    .usingJobData("url", job.getApiEndpoint())
                    .usingJobData("method", job.getMethod())
                    .usingJobData("headers", job.getHeaders())
                    .usingJobData("payload", job.getPayload());

            // Add token if present
            if (job.getToken() != null && !job.getToken().isEmpty()) {
                jobBuilder.usingJobData("token", job.getToken());
            }

            JobDetail jobDetail = jobBuilder.build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(job.getJobName() + "-trigger", "http-triggers")
                    .withSchedule(CronScheduleBuilder.cronSchedule(job.getScheduleExpression()))
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            throw new RuntimeException("Error scheduling job", e);
        }
    }


    /**
     * Persists a new job definition and schedules it.
     *
     * @param job job request payload
     * @param email user email performing the operation
     * @return saved job
     */
    public ScheduledJobs createNewJob(JobRequest job, String email) {
        Optional<ScheduledJobs> sc = repo.findByJobNameAndActiveTrue(job.getJobName());
        if(sc.isPresent()){
            throw new RuntimeException("Job with same Name already exists.");
        }
        ScheduledJobs exists = repo.findByApiEndpointAndMethodAndHeadersAndPayloadAndActiveTrue(
                job.getApiEndpoint(),
                job.getMethod(),
                job.getHeaders(),
                job.getPayload()
        );
        if (exists != null) {
            throw new RuntimeException("Job with same API endpoint, method, Headers, and Payload already exists.");
        }

        job.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        job.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        job.setActive(true);
        Optional<User> user = userRepo.findByEmail(email.toLowerCase());
        job.setCreatedBy(user.get().getId());
        job.setUpdatedBy(user.get().getId());

        ScheduledJobs savedJob = repo.save(Mapper.mapToEntity(job));
        job.setId(savedJob.getId());

        scheduleJob(job);
        return savedJob;
    }

    /**
     * Disables a job and pauses its Quartz schedule.
     */
    public ScheduledJobs pauseJob(Long id, String email) throws SchedulerException {
        ScheduledJobs job = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        job.setEnabled(false);
        Optional<User> user = userRepo.findByEmail(email.toLowerCase());
        job.setUpdatedBy(user.get().getId());
        job.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        repo.save(job);
        scheduler.pauseJob(JobKey.jobKey(job.getJobName(), "http-jobs"));
        return job;
    }

    /**
     * Enables a job and resumes or creates its Quartz schedule if missing.
     */
    public ScheduledJobs resumeJob(Long id, String email) throws SchedulerException {
        ScheduledJobs job = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        job.setEnabled(true);
        Optional<User> user = userRepo.findByEmail(email.toLowerCase());
        job.setUpdatedBy(user.get().getId());
        job.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        repo.save(job);

        JobKey jobKey = JobKey.jobKey(job.getJobName(), "http-jobs");
        if (!scheduler.checkExists(jobKey)) {
            scheduleJob(Mapper.mapToDTO(job));
        } else {
            scheduler.resumeJob(jobKey);
        }
        return job;
    }

    /**
     * Updates a job definition and re-schedules it.
     */
    public ScheduledJobs editJob(JobRequest job, String email) throws SchedulerException {
        ScheduledJobs existingJob = repo.findById(job.getId())
                .orElseThrow(() -> new RuntimeException("Job not found"));

        JobKey jobKey = JobKey.jobKey(existingJob.getJobName(), "http-jobs");
        TriggerKey triggerKey = TriggerKey.triggerKey(existingJob.getJobName() + "-trigger", "http-triggers");

        scheduler.pauseTrigger(triggerKey);
        scheduler.unscheduleJob(triggerKey);
        scheduler.deleteJob(jobKey);

        if (job.getJobName() != null) existingJob.setJobName(job.getJobName());
        if (job.getDescription() != null) existingJob.setDescription(job.getDescription());
        if (job.getScheduleExpression() != null) existingJob.setScheduleExpression(job.getScheduleExpression());
        if (job.getApiEndpoint() != null) existingJob.setApiEndpoint(job.getApiEndpoint());
        if (job.getMethod() != null) existingJob.setMethod(job.getMethod());
        if (job.getHeaders() != null) existingJob.setHeaders(job.getHeaders());
        if (job.getPayload() != null) existingJob.setPayload(job.getPayload());
        if (job.getToken() != null) existingJob.setToken(job.getToken());

        existingJob.setEnabled(job.isEnabled());
        Optional<User> user = userRepo.findByEmail(email.toLowerCase());
        job.setUpdatedBy(user.get().getId());
        existingJob.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        ScheduledJobs updatedJob = repo.save(existingJob);

        if (updatedJob.isActive() && updatedJob.isEnabled()) {
            scheduleJob(Mapper.mapToDTO(existingJob));
        }

        return updatedJob;
    }

    /**
     * Returns active jobs matching the given specification.
     */
    public Page<ScheduledJobs> getAllRoutes(Specification<ScheduledJobs> spec, Pageable pageable) {
        Specification<ScheduledJobs> activeSpec = (root, query, cb) -> cb.isTrue(root.get("active"));
        return repo.findAll(spec.and(activeSpec), pageable);
    }

    /**
     * Soft-deletes a job (active=false), disables it, and pauses scheduling.
     */
    public ScheduledJobs deactivateJob(Long id, String email) throws SchedulerException {
        ScheduledJobs job = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        job.setActive(false);
        job.setEnabled(false);
        Optional<User> user = userRepo.findByEmail(email.toLowerCase());
        job.setUpdatedBy(user.get().getId());
        repo.save(job);
        pauseJob(id,email);
        return job;
    }

    /**
     * Executes a test HTTP call simulating scheduled job behavior, without persisting or scheduling.
     */
    public boolean test(JobRequest job) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        ObjectMapper objectMapper = new ObjectMapper();

        // Parse existing headers
        Map<String, String> headersMap = objectMapper.readValue(job.getHeaders(), new TypeReference<>() {});
        headersMap.forEach(headers::add);

        // Add token if present
        if (job.getToken() != null && !job.getToken().isEmpty()) {
            headers.add("Authorization", "Bearer " + job.getToken());
        }

        // Add test-specific headers
        headers.add("X-Scheduled-Job", "true");
        headers.add("X-Job-Name", job.getJobName() != null ? job.getJobName() : "test-job");

        // Check if this is a retry attempt
        String transactionUUID = extractTransactionUUIDFromUrl(job.getApiEndpoint());
        if (transactionUUID != null && hasBeenUsedBefore(transactionUUID)) {
            headers.add("X-Retry-Attempt", "true");
        }

        HttpEntity<String> entity = new HttpEntity<>(job.getPayload(), headers);

        ResponseEntity<String> response = restTemplate.exchange(
                job.getApiEndpoint(),
                HttpMethod.valueOf(job.getMethod().toUpperCase()),
                entity,
                String.class
        );

        return response.getStatusCode().is2xxSuccessful();
    }


    private String extractTransactionUUIDFromUrl(String url) {
        if (url == null) return null;

        // Extract transactionUUID from URL parameters
        Pattern pattern = Pattern.compile("transactionUUID=([a-fA-F0-9-]+)");
        Matcher matcher = pattern.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    private boolean hasBeenUsedBefore(String transactionUUID) {
        try {
            return middlewareApiCallLogRepository.existsBySourceTransactionUUID(transactionUUID.toLowerCase());
        } catch (Exception e) {
            // Log error but don't fail the test
            System.err.println("Error checking UUID existence: " + e.getMessage());
            return false;
        }
    }

    /**
     * Exports jobs matching spec to CSV or Excel bytes.
     */
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

    /**
     * Returns a view-model of a job including creator/updater emails.
     */
    public ResponseEntity<?> getById(long id) {
        Optional<ScheduledJobs> job = repo.findById(id);
        if(job.isEmpty())
            return ResponseEntity.noContent().build();
        Optional<User> creationUser = userRepo.findById(job.get().getCreatedBy());
        Optional<User> updatingUser = userRepo.findById(job.get().getUpdatedBy());

        return ResponseEntity.ok( SingleJobDto.builder()
                .id(job.get().getId())
                .jobName(job.get().getJobName())
                .description(job.get().getDescription())
                .scheduleExpression(job.get().getScheduleExpression())
                .apiEndpoint(job.get().getApiEndpoint())
                .method(job.get().getMethod())
                .headers(job.get().getHeaders())
                .payload(job.get().getPayload())
                .enabled(job.get().isEnabled())
                .lastExecutionTime(job.get().getLastExecutionTime())
                .createdBy(creationUser.get().getEmail())
                .createdAt(job.get().getCreatedAt())
                .updatedBy(updatingUser.get().getEmail())
                .updatedAt(job.get().getUpdatedAt())
                .active(job.get().isActive()).build()
        );
    }
}