
// Dhis2Response.java
package com.middleware.dhis2.model;

import lombok.Getter;
import lombok.Setter;

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

    public static Dhis2Response success(String message, Object details) {
        Dhis2Response response = new Dhis2Response();
        response.setStatus("SUCCESS");
        response.setMessage(message);
        response.setDetails(details);
        response.setHttpStatusCode(200);
        response.setSuccess(true);
        return response;
    }

    public static Dhis2Response error(int httpCode, String status, String message) {
        Dhis2Response response = new Dhis2Response();
        response.setStatus(status);
        response.setMessage(message);
        response.setHttpStatusCode(httpCode);
        response.setSuccess(false);
        return response;
    }

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