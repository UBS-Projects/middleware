package com.middleware.backend.scheduledJobs.model;

import groovy.beans.Bindable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SingleJobDto {
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
    private String createdBy;
    private Timestamp createdAt;
    private String updatedBy;
    private Timestamp updatedAt;
    private boolean active;
}
