package com.middleware.backend.scheduledJobs.mapper;

import com.middleware.backend.scheduledJobs.DTO.JobRequest;
import com.middleware.backend.scheduledJobs.model.ScheduledJobs;

/**
 * Mapping helpers between {@link JobRequest} DTOs and {@link ScheduledJobs} entities.
 */
public abstract class Mapper {
    /**
     * Creates a new {@link ScheduledJobs} entity from a {@link JobRequest} payload.
     *
     * @param job request DTO containing job definition
     * @return populated entity instance
     */
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
                .active(job.isActive())
                .build();
    }
    /**
     * Creates a {@link JobRequest} DTO from a {@link ScheduledJobs} entity.
     *
     * @param job entity to convert
     * @return DTO representing the entity fields
     */
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
                .active(job.isActive())
                .build();
    }
}
