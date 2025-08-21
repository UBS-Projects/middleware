package com.middleware.backend.audit_logs_interceptor.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
