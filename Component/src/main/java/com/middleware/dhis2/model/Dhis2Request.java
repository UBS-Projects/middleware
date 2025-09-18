// Dhis2Request.java
package com.middleware.dhis2.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Dhis2Request {
    private String transactionUUID;
    private byte[] csvBytes;
    private String scheme = "CODE";
    private String strategy = "NEW_AND_UPDATES";
    private boolean dryRun = true;
    private String contentType;
    private String queryBase;
    private String operation;
    private String responseBody;
    private int httpStatusCode;
}
