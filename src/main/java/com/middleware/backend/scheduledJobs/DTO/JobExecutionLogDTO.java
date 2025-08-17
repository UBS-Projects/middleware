package com.middleware.backend.scheduledJobs.DTO;

import com.middleware.backend.scheduledJobs.model.JobExecutionLogs;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
public class JobExecutionLogDTO {
    private Long id;
    private String jobName;
    private String apiEndpoint;
    private Timestamp startTime;
    private Timestamp endTime;
    private long durationMs;
    private int responseStatus;
    private String status;   // SUCCESS / FAILURE

    public static JobExecutionLogDTO fromEntity(JobExecutionLogs entity) {
        return new JobExecutionLogDTO(
                entity.getId(),
                entity.getJob() != null ? entity.getJob().getJobName() : null,
                entity.getJob() != null ? entity.getJob().getApiEndpoint() : null,
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getDurationMs(),
                entity.getResponseStatus(),
                entity.getStatus() != null ? entity.getStatus().name() : null
        );
    }
}
