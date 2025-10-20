package com.middleware.backend.scheduledJobs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.backend.scheduledJobs.enums.Status;
import com.middleware.backend.scheduledJobs.model.JobExecutionLogs;
import com.middleware.backend.scheduledJobs.model.ScheduledJobs;
import com.middleware.backend.scheduledJobs.repository.JobExecutionLogsRepository;
import com.middleware.backend.scheduledJobs.repository.ScheduledJobRepository;
import lombok.RequiredArgsConstructor;
import org.quartz.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Quartz {@link Job} implementation that executes a configured HTTP request for a scheduled job.
 * <p>
 * Responsibilities:
 * - Build the HTTP request from persisted job details and Quartz job data.
 * - Generate a unique UUID for each execution if the URL contains {{UUID}} placeholder.
 * - Add tracking headers (X-Scheduled-Job, X-Job-Name).
 * - Perform the HTTP call via {@link RestTemplate}.
 * - Persist a {@link JobExecutionLogs} record with request/response and timing.
 * - Update the job's last execution timestamp.
 */
@RequiredArgsConstructor
public class JobExecution implements Job {

    private final RestTemplate restTemplate;
    private final ScheduledJobRepository jobRepo;
    private final JobExecutionLogsRepository logsRepo;

    /**
     * Executes the HTTP call for the scheduled job and writes an execution log.
     *
     * @param context Quartz job context containing job data (url, method, headers, payload)
     * @throws JobExecutionException when an unrecoverable error occurs during execution
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap data = context.getMergedJobDataMap();

        String jobName = context.getJobDetail().getKey().getName();
        String originalUrl = data.getString("url");
        String method = data.getString("method");
        String headers = data.getString("headers");
        String payload = data.getString("payload");

        ProcessedRequest processedRequest = processRequestForExecution(originalUrl, headers, context);

        Timestamp start = Timestamp.from(Instant.now());

        JobExecutionLogs log = new JobExecutionLogs();
        log.setStartTime(start);

        try {
            ScheduledJobs scheduledJob = jobRepo.findByApiEndpointAndMethodAndHeadersAndPayloadAndActiveTrue(
                    originalUrl, method, headers, payload);
            log.setJob(scheduledJob);
            log.setRequestHeaders(processedRequest.headers);
            log.setRequestBody(payload);

            // Prepare HTTP headers
            HttpHeaders httpHeaders = new HttpHeaders();
            if (processedRequest.headers != null && !processedRequest.headers.isEmpty()) {
                Map<String, String> headerMap = new ObjectMapper().readValue(processedRequest.headers, Map.class);
                headerMap.forEach(httpHeaders::add);
            }

            HttpEntity<String> entity = new HttpEntity<>(payload, httpHeaders);

            // Execute HTTP request with the processed URL (UUID replaced if needed)
            ResponseEntity<String> response = restTemplate.exchange(
                    processedRequest.url,
                    HttpMethod.valueOf(method.toUpperCase()),
                    entity,
                    String.class
            );

            // Fill response details
            log.setResponseStatus(response.getStatusCodeValue());
            log.setResponseHeaders(response.getHeaders().toString());
            log.setResponseBody(response.getBody());
            log.setStatus(response.getStatusCode().is2xxSuccessful() ? Status.SUCCESS : Status.FAILURE);

        } catch (Exception e) {
            log.setStatus(Status.FAILURE);
            log.setErrorMessage(e.getMessage());
        } finally {
            Timestamp end = Timestamp.from(Instant.now());
            log.setEndTime(end);
            log.setDurationMs(end.getTime() - start.getTime());

            logsRepo.save(log);

            updateLastExecutionTime(originalUrl, method, headers, payload, end);
        }
    }

    /**
     * Process the request by replacing {{UUID}} placeholder and preparing headers.
     */
    private ProcessedRequest processRequestForExecution(String originalUrl, String originalHeaders, JobExecutionContext context) {
        ProcessedRequest result = new ProcessedRequest();

        // Generate new UUID and replace {{UUID}} placeholder if present
        result.url = replaceUUIDPlaceholder(originalUrl);

        // Fetch the ScheduledJobs entity to access the token
        ScheduledJobs scheduledJob = null;
        try {
            scheduledJob = jobRepo.findByApiEndpointAndMethodAndHeadersAndPayloadAndActiveTrue(
                    originalUrl,
                    context.getJobDetail().getJobDataMap().getString("method"),
                    originalHeaders,
                    context.getJobDetail().getJobDataMap().getString("payload")
            );
        } catch (Exception e) {
            System.err.println("Failed to fetch ScheduledJob for token: " + e.getMessage());
        }

        result.headers = processHeadersForScheduledJob(originalHeaders, context, scheduledJob);

        return result;
    }

    /**
     * Replace {{UUID}} placeholder with a new UUID.
     * Supports both regular and URL-encoded placeholders:
     * - transactionUUID={{UUID}}
     * - transactionUUID=%7B%7BUUID%7D%7D (URL encoded)
     *
     * @param url the original URL that may contain {{UUID}} placeholder
     * @return URL with {{UUID}} replaced by a new UUID, or original URL if no placeholder found
     */
    private String replaceUUIDPlaceholder(String url) {
        if (url == null) return null;

        // Check if placeholder exists
        if (!url.contains("{{UUID}}") && !url.contains("%7B%7BUUID%7D%7D")) {
            return url;
        }

        // Generate new UUID for this execution
        String newUUID = UUID.randomUUID().toString();
        System.out.println("Generated UUID for scheduled job: " + newUUID);

        // Replace regular placeholder
        String finalUrl = url.replace("{{UUID}}", newUUID);

        // Replace URL-encoded placeholder
        finalUrl = finalUrl.replace("%7B%7BUUID%7D%7D", newUUID);

        return finalUrl;
    }

    /**
     * Process headers by adding scheduled job tracking headers and authentication token.
     *
     * NOTE: X-Retry-Attempt header is NOT added for scheduled jobs because each execution
     * generates a new UUID, making every execution unique (not a retry).
     */
    private String processHeadersForScheduledJob(String originalHeaders, JobExecutionContext context, ScheduledJobs scheduledJob) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> headerMap;

            if (originalHeaders != null && !originalHeaders.isEmpty()) {
                headerMap = objectMapper.readValue(originalHeaders, Map.class);
            } else {
                headerMap = new java.util.HashMap<>();
            }

            String jobName = context.getJobDetail().getKey().getName();

            // Add scheduled job identification headers
            headerMap.put("X-Scheduled-Job", "true");
            headerMap.put("X-Job-Name", jobName);

            // Add authentication token if available
            if (scheduledJob != null && scheduledJob.getToken() != null && !scheduledJob.getToken().isEmpty()) {
                headerMap.put("Authorization", "Bearer " + scheduledJob.getToken());
            }

            return objectMapper.writeValueAsString(headerMap);

        } catch (Exception e) {
            String jobName = context.getJobDetail().getKey().getName();
            return String.format("{\"X-Scheduled-Job\":\"true\",\"X-Job-Name\":\"%s\"}", jobName);
        }
    }

    /**
     * Update the last execution time for the scheduled job.
     */
    private void updateLastExecutionTime(String url, String method, String headers,
                                         String payload, Timestamp executionTime) {
        try {
            ScheduledJobs job = jobRepo.findByApiEndpointAndMethodAndHeadersAndPayloadAndActiveTrue(
                    url, method, headers, payload);
            if (job != null) {
                job.setLastExecutionTime(executionTime);
                jobRepo.save(job);
            }
        } catch (Exception e) {
            System.err.println("Failed to update last execution time: " + e.getMessage());
        }
    }

    private static class ProcessedRequest {
        String url;
        String headers;
    }
}