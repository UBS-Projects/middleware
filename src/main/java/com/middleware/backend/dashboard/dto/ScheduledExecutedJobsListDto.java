package com.middleware.backend.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * Data Transfer Object representing a summary of executed scheduled jobs.
 * <p>
 * Used to transfer job execution details such as name, status, execution time, and duration
 * for dashboard display or reporting purposes.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ScheduledExecutedJobsListDto {

    /**
     * Unique identifier of the executed job.
     */
    private long id;

    /**
     * Name of the executed scheduled job.
     */
    private String name;

    /**
     * Status of the job execution (e.g., SUCCESS, FAILED).
     */
    private String status;

    /**
     * Timestamp indicating when the job was executed.
     */
    private Timestamp executionTime;

    /**
     * Duration of the job execution in milliseconds.
     */
    private Long duration;
}
