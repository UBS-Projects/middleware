package com.middleware.backend.model;

import java.time.LocalDateTime;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "destination_api")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_template", columnDefinition = "jsonb")
    private Map<String, Object> inputTemplate;

  @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_header_template", columnDefinition = "jsonb")
    private Map<String, Object> inputHeaderTemplate;

 @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "query_params", columnDefinition = "jsonb")
    private Map<String, Object> queryParams;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_template", columnDefinition = "jsonb")
    private Map<String, Object> outputTemplate;

    @Column(name = "auth_type", length = 50)
    private String authType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "auth_credentials", columnDefinition = "jsonb")
    private Map<String, Object> authCredentials;

   @JdbcTypeCode(SqlTypes.JSON)  //@Convert(converter = JsonbConverter.class)
    @Column(name = "headers" , columnDefinition = "jsonb")
    private Map<String, Object> headers;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
