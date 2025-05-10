package com.middleware.backend.model;


import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "destination_api")
@Data
public class DestinationApi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "base_uri", nullable = false, length = 500)
    private String baseUri;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "input_template", columnDefinition = "TEXT")
    private String inputTemplate;
    @Column(name = "input_header_template", columnDefinition = "TEXT")
    private Map<String, String> inputHeaderTemplate;

    @Column(name = "output_template", columnDefinition = "TEXT")
    private String outputTemplate;

    @Column(name = "auth_type", length = 50)
    private String authType;

    @Column(name = "auth_credentials", columnDefinition = "TEXT")
    private String authCredentials;

    @Column(columnDefinition = "JSONB")
    @Convert(converter = JsonbConverter.class)
    private Map<String, String> headers;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
