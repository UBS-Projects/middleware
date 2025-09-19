package com.middleware.backend.scheduledJobs.mapper;

import com.middleware.backend.scheduledJobs.DTO.JobExecutionDTO;
import com.middleware.backend.scheduledJobs.model.ExecutionHistory;
import com.middleware.backend.scheduledJobs.service.JobExecution;

/**
 * Mapper utilities for converting between execution audit DTOs and entities.
 */
public class LogsMapper {

    /**
     * Maps a {@link JobExecutionDTO} audit record to an {@link ExecutionHistory} entity.
     *
     * @param job the audit DTO to map
     * @return a new {@code ExecutionHistory} entity with copied fields
     */
    public static ExecutionHistory MapToEntity(JobExecutionDTO job){
        return ExecutionHistory.builder()
                .id(job.getId())
                .scheduledJobName(job.getScheduledJobName())
                .scheduledJobMethod(job.getScheduledJobMethod())
                .scheduledJobPath(job.getScheduledJobPath())
                .status(job.getStatus())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .action(job.getAction())
                .userEmail(job.getUserEmail())
                .build();
    }

    /**
     * Maps an {@link ExecutionHistory} entity to a {@link JobExecutionDTO} audit DTO.
     *
     * @param job the entity to map
     * @return a new {@code JobExecutionDTO} with copied fields
     */
    public static JobExecutionDTO MapToDto(ExecutionHistory job){
        return JobExecutionDTO.builder()
                .id(job.getId())
                .scheduledJobName(job.getScheduledJobName())
                .scheduledJobMethod(job.getScheduledJobMethod())
                .scheduledJobPath(job.getScheduledJobPath())
                .status(job.getStatus())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .action(job.getAction())
                .userEmail(job.getUserEmail())
                .build();
    }
}
