package com.middleware.backend.model;

import java.time.LocalDateTime;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "api_endpoint")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "base_url", nullable = true)
    private String baseUrl;

    @Column(name = "endpoint_path", nullable = false)
    private String endpointPath;

    @Column(name = "http_method", nullable = false)
    private String method;

    @Column(name = "authentication_type")
    private String authenticationType;

    //@Convert(converter = JsonbConverter.class)
     @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_template", columnDefinition = "jsonb")
    private Map<String, Object> inputTemplate;

      //@Convert(converter = JsonbConverter.class)
      @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_header", columnDefinition = "jsonb")
    private Map<String, Object> inputheader;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_template", columnDefinition = "jsonb")
    private Map<String, Object> outputTemplate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_headers", columnDefinition = "jsonb")
    private Map<String, Object> outputheaders;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "query_params", columnDefinition = "jsonb")
    private Map<String, Object> queryParams;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trigger_workflow_id", nullable=true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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
