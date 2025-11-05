package com.middleware.component.errormapper.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the essential details of an error mapping entry.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ErrorMappingDetail {
    private Long id;
    private String routeId;
    private String mappedErrorCode;
    private String mappedMessage;
    private Integer httpStatusCode;
    private String language;
    private Boolean active;
}