package com.middleware.backend.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ScheduledExecutedJobsListDto {
    private long id;
    private String name;
    private String status;
    private Timestamp executionTime;
    private Long duration;
}
