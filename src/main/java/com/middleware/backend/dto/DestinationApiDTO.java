package com.middleware.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class DestinationApiDTO {
    private Long id;
    private String name;
    private String baseUri;
    private String httpMethod;
    private Map<String, Object> inputTemplate;
    private Map<String, Object> inputHeaderTemplate;
    private Map<String, Object> queryParams;
    private Map<String, Object> outputTemplate;
    private String authType;
    private Map<String, Object> authCredentials;
    private Map<String, Object> headers;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
