package com.middleware.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WorkflowConfigDTO {
    private Long id;
    private String name;
    private String description;
    private Boolean active;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
