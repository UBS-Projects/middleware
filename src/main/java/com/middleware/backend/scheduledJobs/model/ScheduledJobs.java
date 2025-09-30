package com.middleware.backend.scheduledJobs.model;

import com.middleware.backend.scheduledJobs.enums.ScheduleExpression;
import com.middleware.backend.scheduledJobs.enums.ScheduleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * Entity representing a scheduled job definition persisted in the database.
 * Stores scheduling expression, target API endpoint, request details, and audit metadata.
 */
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


    @Column(name = "schedule_expression")
    private String scheduleExpression;

    @Column(name = "api_endpoint",nullable = false)
    private String apiEndpoint;

    private String method;


    @Column(length = 1000)
    private String headers;
    @Column(length = 1000)
    private String payload;

    private boolean enabled;


    @Column(name = "token",length = 1000)
    private String token;

    @Column(name = "last_execution_time")
    private Timestamp lastExecutionTime;


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
