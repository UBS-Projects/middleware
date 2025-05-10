package com.middleware.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse {
    
    // Actual response body (can be any structure)
    private Object body;

    // HTTP status code (e.g., 200, 404)
    private HttpStatus status;

    // Headers to return to the client
    private HttpHeaders headers;

    public static ApiResponse ok(Object body) {
        return ApiResponse.builder()
                .body(body)
                .status(HttpStatus.OK)
                .headers(new HttpHeaders())
                .build();
    }

    public static ApiResponse error(Object body, HttpStatus status) {
        return ApiResponse.builder()
                .body(body)
                .status(status)
                .headers(new HttpHeaders())
                .build();
    }

    public static ApiResponse withHeaders(Object body, HttpStatus status, Map<String, String> headerMap) {
        HttpHeaders headers = new HttpHeaders();
        headerMap.forEach(headers::add);
        return ApiResponse.builder()
                .body(body)
                .status(status)
                .headers(headers)
                .build();
    }

    
}
