package com.middleware.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MappingResponse {
    private String status;
    private String message;
    private Object data;
    private int httpStatusCode;
    private boolean success;

    public static MappingResponse success(String message, Object data) {
        MappingResponse response = new MappingResponse();
        response.setStatus("SUCCESS");
        response.setMessage(message);
        response.setData(data);
        response.setHttpStatusCode(200);
        response.setSuccess(true);
        return response;
    }

    public static MappingResponse error(int httpCode, String status, String message) {
        MappingResponse response = new MappingResponse();
        response.setStatus(status);
        response.setMessage(message);
        response.setHttpStatusCode(httpCode);
        response.setSuccess(false);
        return response;
    }
}