package com.middleware.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Map;



@Entity
@Table(name = "api_endpoint")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "base_url", nullable = false)
    private String baseUrl;

    @Column(name = "endpoint_path", nullable = false)
    private String endpointPath;

    @Column(name = "http_method", nullable = false)
    private String method;

    @Column(name = "authentication_type")
    private String authenticationType;

    @Column(name = "input_template", columnDefinition = "TEXT")
    private String inputTemplate;

    @Column(name = "output_template", columnDefinition = "TEXT")
    private String outputTemplate;


   // @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> headers;

   // @Type(type = "jsonb")
    @Column(name = "query_params", columnDefinition = "jsonb")
    private Map<String, Object> queryParams;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trigger_workflow_id")
    private WorkflowConfig triggerWorkflow;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
