// Dhis2Request.java
package com.middleware.dhis2.model;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO carrying parameters and payload for DHIS2 operations.
 * <p>
 * Populated from Camel message headers/body by the producer and consumed by
 * the DHIS2 service.
 */
@Getter
@Setter
public class Dhis2Request {
    /** Unique transaction identifier propagated via headers. */
    private String transactionUUID;
    /** CSV payload bytes for upload/validation. */
    private byte[] csvBytes;
    /** ID scheme (CODE or UID). */
    private String scheme = "CODE";
    /** Import strategy (NEW, UPDATES, NEW_AND_UPDATES, DELETE). */
    private String strategy = "NEW_AND_UPDATES";
    /** Whether to perform dry-run validation before import. */
    private boolean dryRun = true;
    /** Content type of the incoming body, defaults to text/csv when bytes present. */
    private String contentType;
    /** Base query string used when invoking DHIS2 import endpoints. */
    private String queryBase;
    /** Operation requested (upload, guard, summarize, process/complete). */
    private String operation;
    /** Response body from upstream DHIS2 when summarizing an import. */
    private String responseBody;
    /** HTTP status code from upstream DHIS2 when summarizing an import. */
    private int httpStatusCode;
}
