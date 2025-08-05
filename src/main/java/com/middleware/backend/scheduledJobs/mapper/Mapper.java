package com.middleware.backend.scheduledJobs.mapper;

import com.middleware.backend.scheduledJobs.DTO.JobRequest;
import com.middleware.backend.scheduledJobs.model.ScheduledJobs;

public abstract class Mapper {
    public static ScheduledJobs mapToEntity(JobRequest job){
        return ScheduledJobs.builder()
                .jobName(job.getJobName())
                .description(job.getDescription())
                .scheduleExpression(job.getScheduleExpression())
                .apiEndpoint(job.getApiEndpoint())
                .headers(job.getHeaders())
                .payload(job.getPayload())
                .enabled(job.isEnabled())
                .lastExecutionTime(job.getLastExecutionTime())
                .createdBy(job.getCreatedBy())
                .createdAt(job.getCreatedAt())
                .updatedBy(job.getUpdatedBy())
                .updatedAt(job.getUpdatedAt())
                .method(job.getMethod())
                .build();
    }
    public static JobRequest mapToDTO (ScheduledJobs job){
        return JobRequest.builder()
                .id(job.getId())
                .jobName(job.getJobName())
                .description(job.getDescription())
                .scheduleExpression(job.getScheduleExpression())
                .apiEndpoint(job.getApiEndpoint())
                .headers(job.getHeaders())
                .payload(job.getPayload())
                .enabled(job.isEnabled())
                .lastExecutionTime(job.getLastExecutionTime())
                .createdBy(job.getCreatedBy())
                .createdAt(job.getCreatedAt())
                .updatedBy(job.getUpdatedBy())
                .updatedAt(job.getUpdatedAt())
                .method(job.getMethod())
                .build();
    }
}
