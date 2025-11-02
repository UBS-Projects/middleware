package com.middleware.backend.scheduledJobs.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.backend.scheduledJobs.DTO.JobRequest;
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
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.quartz.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@AllArgsConstructor
public class ScheduledJobsService{
    private final ScheduledJobRepository repo;
    private Scheduler scheduler;
    private final RestTemplate restTemplate;
    private final UserRepository userRepo;
    private final MiddlewareApiCallLogRepository middlewareApiCallLogRepository;

    public void scheduleAllActiveJobs() {
        List<ScheduledJobs> jobs = repo.findByEnabledTrue();
        List<JobRequest> jobDTOs = jobs.stream()
                .map(Mapper::mapToDTO)
                .toList();
        jobDTOs.forEach(this::scheduleJob);
    }

    private void scheduleJob(JobRequest job) {
        try {
            JobBuilder jobBuilder = JobBuilder.newJob(JobExecution.class)
                    .withIdentity(job.getJobName(), "http-jobs")
                    .usingJobData("url", job.getApiEndpoint())
                    .usingJobData("method", job.getMethod())
                    .usingJobData("headers", job.getHeaders())
                    .usingJobData("payload", job.getPayload());

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

    public Page<ScheduledJobs> getAllRoutes(Specification<ScheduledJobs> spec, Pageable pageable) {
        Specification<ScheduledJobs> activeSpec = (root, query, cb) -> cb.isTrue(root.get("active"));
        return repo.findAll(spec.and(activeSpec), pageable);
    }

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

         headers.add("X-Scheduled-Job", "true");
        headers.add("X-Job-Name", job.getJobName() != null ? job.getJobName() : "test-job");

         String finalUrl = replaceUUIDPlaceholderForTest(job.getApiEndpoint());

         String transactionUUID = extractTransactionUUIDFromUrl(finalUrl);
        if (transactionUUID != null && hasBeenUsedBefore(transactionUUID)) {
            headers.add("X-Retry-Attempt", "true");
        }

        HttpEntity<String> entity = new HttpEntity<>(job.getPayload(), headers);

        ResponseEntity<String> response = restTemplate.exchange(
                finalUrl,
                HttpMethod.valueOf(job.getMethod().toUpperCase()),
                entity,
                String.class
        );

        return response.getStatusCode().is2xxSuccessful();
    }


    private String replaceUUIDPlaceholderForTest(String url) {
        if (url == null) return null;

         if (url.contains("{{UUID}}") || url.contains("%7B%7BUUID%7D%7D")) {
            String newUUID = UUID.randomUUID().toString();

             String finalUrl = url.replace("{{UUID}}", newUUID);

             finalUrl = finalUrl.replace("%7B%7BUUID%7D%7D", newUUID);

            return finalUrl;
        }

        return url;
    }

    private String extractTransactionUUIDFromUrl(String url) {
        if (url == null) return null;

        Pattern pattern = Pattern.compile("transactionUUID=([a-fA-F0-9-]+)");
        Matcher matcher = pattern.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    private boolean hasBeenUsedBefore(String transactionUUID) {
        try {
            return middlewareApiCallLogRepository.existsBySourceTransactionUUID(transactionUUID.toLowerCase());
        } catch (Exception e) {
            System.err.println("Error checking UUID existence: " + e.getMessage());
            return false;
        }
    }

    /**
     * Exports jobs matching spec to CSV or Excel bytes.
     */
    public byte[] exportFile(Specification<ScheduledJobs> spec, String type, String sortedBy, String sortDirection) throws IOException {
        Specification<ScheduledJobs> activeSpec = (root, query, cb) -> cb.isTrue(root.get("active"));
        spec = spec.and(activeSpec);

        if ("CSV".equalsIgnoreCase(type)) {
            return convertToCSVStreamed(spec, sortedBy,sortDirection);
        } else if ("Excel".equalsIgnoreCase(type) || "XLSX".equalsIgnoreCase(type)) {
            return convertToExcelStreamed(spec, sortedBy,sortDirection);
        } else {
            throw new IllegalArgumentException("Unsupported export type: " + type);
        }
    }


    // Helper method to convert List<ScheduledJobs> to CSV String
    private byte[] convertToCSVStreamed(Specification<ScheduledJobs> spec, String sortedBy, String sortDirection) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(bos, StandardCharsets.UTF_8))) {
            // Write CSV headers
            writer.println("JobName,ApiEndpoint,Method,Enabled,Active,UpdatedAt");
            writer.flush();

            int pageSize = 1000;
            int pageNumber = 0;
            boolean hasMore = true;

            while (hasMore) {
                Sort sort = sortDirection.equalsIgnoreCase("asc")
                        ? Sort.by(sortedBy).ascending()
                        : Sort.by(sortedBy).descending();
                Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
                Page<ScheduledJobs> page = repo.findAll(spec, pageable);

                for (ScheduledJobs job : page.getContent()) {
                    writer.append(escapeCsv(job.getJobName())).append(",");
                    writer.append(escapeCsv(job.getApiEndpoint())).append(",");
                    writer.append(escapeCsv(job.getMethod())).append(",");
                    writer.append(String.valueOf(job.isEnabled())).append(",");
                    writer.append(String.valueOf(job.isActive())).append(",");
                    writer.append(job.getUpdatedAt() != null ? job.getUpdatedAt().toString() : "").append("\n");
                }

                writer.flush();
                hasMore = page.hasNext();
                pageNumber++;
            }

            writer.flush();
        }

        return bos.toByteArray();
    }


    private String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private String safeString(String value) {
        return value != null ? value : "";
    }


    // Helper method to convert List<ScheduledJobs> to Excel bytes
    private byte[] convertToExcelStreamed(Specification<ScheduledJobs> spec, String sortedBy, String sortDirection) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        SXSSFWorkbook workbook = new SXSSFWorkbook(100); // Keep 100 rows in memory
        Sheet sheet = workbook.createSheet("Scheduled Jobs");

        try {
            // Header row
            Row header = sheet.createRow(0);
            String[] headers = {"JobName", "ApiEndpoint", "Method", "Enabled", "Active", "UpdatedAt"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            int[] widths = {25, 40, 15, 10, 10, 25};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            int rowIdx = 1;
            int pageSize = 1000;
            int pageNumber = 0;
            boolean hasMore = true;

            while (hasMore) {
                Sort sort = sortDirection.equalsIgnoreCase("asc")
                        ? Sort.by(sortedBy).ascending()
                        : Sort.by(sortedBy).descending();
                Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
                Page<ScheduledJobs> page = repo.findAll(spec, pageable);

                for (ScheduledJobs job : page.getContent()) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(safeString(job.getJobName()));
                    row.createCell(1).setCellValue(safeString(job.getApiEndpoint()));
                    row.createCell(2).setCellValue(safeString(job.getMethod()));
                    row.createCell(3).setCellValue(job.isEnabled());
                    row.createCell(4).setCellValue(job.isActive());
                    row.createCell(5).setCellValue(job.getUpdatedAt() != null ? job.getUpdatedAt().toString() : "");
                }

                hasMore = page.hasNext();
                pageNumber++;
            }

            workbook.write(bos);
            return bos.toByteArray();

        } finally {
            workbook.dispose();
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