package com.middleware.backend.dto;

import com.middleware.backend.model.ErrorMapping;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ErrorMappingDto {
    private Long id;
    private String routeId;
    private String routePath;

     private Long sourceSystemId;
    private String sourceSystemName;

    private String rawErrorSubstring;
    private ErrorMapping.MatchType matchType;
    private String mappedErrorCode;
    private String mappedMessage;
    private Long errorCategoryId;
    private String errorCategoryName;
    private Integer httpStatusCode;
    private String language;
    private Boolean active;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}