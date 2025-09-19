package com.middleware.backend.scheduledJobs.DTO;

import com.middleware.backend.scheduledJobs.enums.Status;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * DTO representing an audit record for an operation performed on a scheduled job.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class JobExecutionDTO {
    private Long id;
    private String scheduledJobName;
    private String scheduledJobMethod;
    private String scheduledJobPath;
    private Status status;
    private String errorMessage;
    private String action;
    private Timestamp createdAt;
    private String userEmail;
}