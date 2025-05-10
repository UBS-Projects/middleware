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
    private String inputTemplate;
    private Map<String, String> inputHeaderTemplate;
    private String outputTemplate;
    private String authType;
    private String authCredentials;
    private Map<String, String> headers;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
