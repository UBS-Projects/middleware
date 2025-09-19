package com.middleware.backend.scheduledJobs.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.middleware.backend.scheduledJobs.enums.Status;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

/**
 * Entity capturing a single execution of a scheduled job including request/response payloads.
 * Linked to {@link ScheduledJobs} and records timing, status, and error details.
 */
@Entity
@Table(name = "job_execution_logs")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JobExecutionLogs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "job_id")
    private ScheduledJobs job;

    private Timestamp startTime;
    private Timestamp endTime;
    private long durationMs;

    @Lob
    private String requestHeaders;

    @Lob
    private String requestBody;

    @Lob
    private String responseHeaders;

    @Lob
    private String responseBody;

    private int responseStatus;

    @Enumerated(EnumType.STRING)
    private Status status; // SUCCESS / FAILURE
    @Column(length = 1000)
    private String errorMessage;
}
