
// Dhis2Response.java
package com.middleware.dhis2.model;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO representing the outcome of a DHIS2 operation.
 * <p>
 * Includes HTTP status, status text, message, optional details payload, and a
 * success flag used by the producer to set response metadata.
 */
@Getter
@Setter
public class Dhis2Response {
    private String status;
    private String message;
    private String errorCode;
    private String errorMessage;
    private Object details;
    private int httpStatusCode;
    private boolean success;

    /** Creates a 200 SUCCESS response with an optional details object. */
    public static Dhis2Response success(String message, Object details) {
        Dhis2Response response = new Dhis2Response();
        response.setStatus("SUCCESS");
        response.setMessage(message);
        response.setDetails(details);
        response.setHttpStatusCode(200);
        response.setSuccess(true);
        return response;
    }

    /** Creates an error response with the given HTTP code and status label. */
    public static Dhis2Response error(int httpCode, String status, String message) {
        Dhis2Response response = new Dhis2Response();
        response.setStatus(status);
        response.setMessage(message);
        response.setHttpStatusCode(httpCode);
        response.setSuccess(false);
        return response;
    }

    /** Creates a 409 CONFLICT response with a details payload. */
    public static Dhis2Response conflict(String message, Object details) {
        Dhis2Response response = new Dhis2Response();
        response.setStatus("CONFLICT");
        response.setMessage(message);
        response.setDetails(details);
        response.setHttpStatusCode(409);
        response.setSuccess(false);
        return response;
    }
}