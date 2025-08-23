package com.middleware.backend.scheduledJobs.mapper;

import com.middleware.backend.scheduledJobs.DTO.JobExecutionDTO;
import com.middleware.backend.scheduledJobs.model.ExecutionHistory;
import com.middleware.backend.scheduledJobs.service.JobExecution;

public class LogsMapper {

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
