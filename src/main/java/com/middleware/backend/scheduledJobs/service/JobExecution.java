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

@RequiredArgsConstructor
public class JobExecution implements Job {

    private final RestTemplate restTemplate;
    private final ScheduledJobRepository jobRepo;
    private final JobExecutionLogsRepository logsRepo;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap data = context.getMergedJobDataMap();

        String jobName = context.getJobDetail().getKey().getName();
        String url = data.getString("url");
        String method = data.getString("method");
        String headers = data.getString("headers");
        String payload = data.getString("payload");

        Timestamp start = Timestamp.from(Instant.now());

        JobExecutionLogs log = new JobExecutionLogs();
        log.setStartTime(start);

        try {
            ScheduledJobs scheduledJob = jobRepo.findByApiEndpointAndMethodAndActiveTrue(url,method);
            log.setJob(scheduledJob);
            log.setRequestHeaders(headers);
            log.setRequestBody(payload);

            // Prepare headers
            HttpHeaders httpHeaders = new HttpHeaders();
            if (headers != null && !headers.isEmpty()) {
                Map<String, String> headerMap = new ObjectMapper().readValue(headers, Map.class);
                headerMap.forEach(httpHeaders::add);
            }

            HttpEntity<String> entity = new HttpEntity<>(payload, httpHeaders);

            // Execute request
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
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
        }
    }
}
