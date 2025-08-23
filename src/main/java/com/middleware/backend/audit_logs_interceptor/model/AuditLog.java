package com.middleware.backend.audit_logs_interceptor.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userName;
    private String method;
    private String apiPath;
    private String queryString;
    private Integer responseStatus;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String requestHeaders;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String requestBody;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String responseHeaders;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String responseBody;
}
