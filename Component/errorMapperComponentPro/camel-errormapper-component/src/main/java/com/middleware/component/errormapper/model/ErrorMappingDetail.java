package com.middleware.component.errormapper.model;

import lombok.Getter;
import lombok.Setter;

/**
 * A data transfer object to hold the details of an error mapping, including the
 * raw error message, mapped code, and HTTP status.
 */
@Getter
@Setter
public class ErrorMappingDetail {

    private String routeId;
    private String routePath;
    private String rawErrorSubstring;
    private String matchType;
    private String mappedErrorCode;
    private String mappedMessage;
    private Integer httpStatusCode;
    private String sourceSystem;
    private String errorCategory;
    private String language;
    private Boolean active;
}
