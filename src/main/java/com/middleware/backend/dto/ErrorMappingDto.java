package com.middleware.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ErrorMappingDto {
    private Long id;
    private Long destinationApiId;
    private String destinationSystemName;
    private String rawErrorSubstring;
    private String matchType;
    private String mappedErrorCode;
    private String mappedMessage;
    private String errorCategory;
    private Integer httpStatusCode;
    private String language;
    private Boolean active;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
