package com.middleware.backend.scheduledJobs.DTO;

import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Builder
public class JobRequest {
    private Long id;

    private String jobName;

    private String description;

    private String scheduleExpression;

    private String apiEndpoint;

    private String method;

    private String headers;

    private String payload;

    private boolean enabled;

    private Timestamp lastExecutionTime;

    private Long createdBy;

    private Timestamp createdAt;

    private Long updatedBy;


    private Timestamp updatedAt;

    private boolean active = true;

}
