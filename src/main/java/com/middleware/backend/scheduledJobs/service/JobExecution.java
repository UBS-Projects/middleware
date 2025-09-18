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

@RequiredArgsConstructor
public class JobExecution implements Job {

    private final RestTemplate restTemplate;
    private final ScheduledJobRepository jobRepo;
    private final JobExecutionLogsRepository logsRepo;

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

            // Prepare headers
            HttpHeaders httpHeaders = new HttpHeaders();
            if (processedRequest.headers != null && !processedRequest.headers.isEmpty()) {
                Map<String, String> headerMap = new ObjectMapper().readValue(processedRequest.headers, Map.class);
                headerMap.forEach(httpHeaders::add);
            }

            HttpEntity<String> entity = new HttpEntity<>(payload, httpHeaders);

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

    private ProcessedRequest processRequestForExecution(String originalUrl, String originalHeaders, JobExecutionContext context) {
        ProcessedRequest result = new ProcessedRequest();

        if (!isInternalCamelEndpoint(originalUrl)) {
            result.url = originalUrl;
            result.headers = originalHeaders;
            return result;
        }

        result.url = originalUrl;

        result.headers = processHeadersForScheduledJob(originalHeaders, context);

        return result;
    }

    private String processHeadersForScheduledJob(String originalHeaders, JobExecutionContext context) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> headerMap;

            if (originalHeaders != null && !originalHeaders.isEmpty()) {
                headerMap = objectMapper.readValue(originalHeaders, Map.class);
            } else {
                headerMap = new java.util.HashMap<>();
            }

            String jobName = context.getJobDetail().getKey().getName();
            headerMap.put("X-Scheduled-Job", "true");
            headerMap.put("X-Job-Name", jobName);

            boolean isFirstExecution = isFirstExecution(context);
            if (!isFirstExecution) {
                headerMap.put("X-Retry-Attempt", "true");
            }

            return objectMapper.writeValueAsString(headerMap);

        } catch (Exception e) {
            String jobName = context.getJobDetail().getKey().getName();
            return String.format("{\"X-Scheduled-Job\":\"true\",\"X-Job-Name\":\"%s\"}", jobName);
        }
    }

    private boolean isFirstExecution(JobExecutionContext context) {
        return context.getPreviousFireTime() == null;
    }

    private boolean isInternalCamelEndpoint(String url) {
        if (url == null) return false;

        return url.contains("/camel/") ||
                url.contains("transactionUUID=") ||
                url.contains("/v1/datasets") ||
                url.contains("/v1/integrate");
    }

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
            // Log error but don't fail the job execution
            System.err.println("Failed to update last execution time: " + e.getMessage());
        }
    }

    private static class ProcessedRequest {
        String url;
        String headers;
    }
}