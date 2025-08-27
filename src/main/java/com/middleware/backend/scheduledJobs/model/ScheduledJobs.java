package com.middleware.backend.scheduledJobs.model;

import com.middleware.backend.scheduledJobs.enums.ScheduleExpression;
import com.middleware.backend.scheduledJobs.enums.ScheduleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "job_schedules")
@Builder
public class ScheduledJobs {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name")
    private String jobName;

    private String description;

//    @Column(name = "workflow_id")
//    private Long workflowid;
//
//    @Column(name = "api_id")
//    private Long apiEndpointId;

//    @Column(name = "schedule_type", nullable = false)
//    private ScheduleType scheduleType;

    @Column(name = "schedule_expression")
    private String scheduleExpression;

//    @Column(name = "event_trigger")
//    private String eventTrigger;

    @Column(name = "api_endpoint",nullable = false)
    private String apiEndpoint;

    private String method;


    @Column(length = 1000)
    private String headers;
    @Column(length = 1000)
    private String payload;

    private boolean enabled;

//    @Column(name = "max_retry_count")
//    private int maxRetryCount;

//    @Column(name = "retry_innterval_ms")
//    private int retryIntervalMs;

    @Column(name = "last_execution_time")
    private Timestamp lastExecutionTime;

//    @Column(name = "next_execution_time")
//    private Timestamp nextExecutionTime;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;

    private boolean active = true;



}
