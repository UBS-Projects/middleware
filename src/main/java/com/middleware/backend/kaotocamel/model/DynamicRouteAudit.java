package com.middleware.backend.kaotocamel.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dynamic_route_audit")
@Data
@NoArgsConstructor
public class DynamicRouteAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String routeId;

    private int version;

    private String action; // upload, start, stop, revert, deactivate

    @Column(name = "details", columnDefinition = "TEXT")
    private String details; // Additional info (optional)

    private LocalDateTime timestamp;

    @Column(name = "userEmail", columnDefinition = "TEXT")
    private String userEmail;
    private String status;
}
