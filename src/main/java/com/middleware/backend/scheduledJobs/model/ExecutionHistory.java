package com.middleware.backend.scheduledJobs.model;

import com.middleware.backend.scheduledJobs.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "job_executions")
public class ExecutionHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scheduled_job_id", nullable = false)
    private Long scheduledJobId;

    private Status status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "start_time", nullable = false)
    private Timestamp startTime;


    @Column(name = "end_time")
    private Timestamp endTime;

    @Column(name = "execution_time_ms")
    private Long ExecutionTimeMs;

    @Column(name = "retry_count")
    private int retryCount;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;
}
