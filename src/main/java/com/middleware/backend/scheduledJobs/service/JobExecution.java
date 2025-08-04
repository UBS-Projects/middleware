package com.middleware.backend.scheduledJobs.service;

import lombok.AllArgsConstructor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.quartz.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@AllArgsConstructor
public class JobExecution implements Job {
    private final RestTemplate restTemplate;
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap data = context.getMergedJobDataMap();

        String url = data.getString("url");
        String method = data.getString("method"); // Optional: default POST
        String headers = data.getString("headers");
        String payload = data.getString("payload");

        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            if (headers != null && !headers.isEmpty()) {
                // Expecting headers in JSON format
                Map<String, String> headerMap = new ObjectMapper().readValue(headers, Map.class);
                headerMap.forEach(httpHeaders::add);
            }

            HttpEntity<String> entity = new HttpEntity<>(payload, httpHeaders);
            System.out.println("********************************************");
            System.out.println(method);
            restTemplate.exchange(url, HttpMethod.valueOf(method.toUpperCase()), entity, String.class); // assuming POST
        } catch (Exception e) {
            throw new JobExecutionException("HTTP API Job failed", e);
        }
    }
}
