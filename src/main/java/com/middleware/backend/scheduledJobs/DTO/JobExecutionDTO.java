package com.middleware.backend.scheduledJobs.DTO;

import com.middleware.backend.scheduledJobs.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class JobExecutionDTO {
    private Long id;
    private Long scheduledJobId;
    private Status status;
    private String errorMessage;
    private Timestamp startTime;
    private Timestamp endTime;
    private Long ExecutionTimeMs;
    private Timestamp createdAt;
}