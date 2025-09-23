package com.middleware.backend.scheduledJobs.DTO;

import com.middleware.backend.scheduledJobs.model.JobExecutionLogs;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Timestamp;

/**
 * Detailed DTO for returning a single job execution log including request/response data.
 */
@Data
@AllArgsConstructor
public class LogResponse {
    private Long id;
    private String jobName;
    private String apiEndpoint;
    private Timestamp startTime;
    private Timestamp endTime;
    private long durationMs;
    private String requestHeaders;
    private String requestBody;
    private String responseHeaders;
    private String responseBody;
    private int responseStatus;
    private String status;   // SUCCESS / FAILURE
    private String errorMessage;

    /**
     * Maps a {@link JobExecutionLogs} entity to a detailed response DTO.
     *
     * @param entity source execution log entity
     * @return mapped {@code LogResponse}
     */
    public static LogResponse fromEntity(JobExecutionLogs entity) {
        return new LogResponse(
                entity.getId(),
                entity.getJob() != null ? entity.getJob().getJobName() : null,
                entity.getJob() != null ? entity.getJob().getApiEndpoint() : null,
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getDurationMs(),
                entity.getRequestHeaders(),
                entity.getRequestBody(),
                entity.getResponseHeaders(),
                entity.getResponseBody(),
                entity.getResponseStatus(),
                entity.getStatus() != null ? entity.getStatus().name() : null,
                entity.getErrorMessage()
        );
    }
}
