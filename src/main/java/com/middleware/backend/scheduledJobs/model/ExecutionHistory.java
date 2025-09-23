package com.middleware.backend.scheduledJobs.model;

import com.middleware.backend.scheduledJobs.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * Entity auditing user actions performed on scheduled job definitions (create/edit/etc.).
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Table(name = "job_executions")
public class ExecutionHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scheduled_job_name", nullable = false)
    private String scheduledJobName;//jobname

    @Column(name = "scheduled_job_method", nullable = false)
    private String scheduledJobMethod;//jobMethod.

    @Column(name = "scheduled_job_path", nullable = false)
    private String scheduledJobPath;//API.

    private String action;

    private Status status;

    @Column(name = "error_message")
    private String errorMessage;


    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "user_email")
    private String userEmail;
}
