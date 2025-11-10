package com.middleware.backend.errormapping.dto;

import java.time.LocalDateTime;

import com.middleware.backend.errormapping.model.ErrorMapping;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorMappingDto {
    private Long id;
    private String routeId;
    private String code;
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
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}