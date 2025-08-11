package com.middleware.backend.scheduledJobs.mapper;

import com.middleware.backend.scheduledJobs.DTO.JobExecutionDTO;
import com.middleware.backend.scheduledJobs.model.ExecutionHistory;
import com.middleware.backend.scheduledJobs.service.JobExecution;

public class LogsMapper {
    public static JobExecutionDTO MapToDto(ExecutionHistory job){
        return JobExecutionDTO.builder()
                .id(job.getId())

                .build();
    }
}
