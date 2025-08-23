package com.middleware.backend.kaotocamel.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    private String details; // Additional info (optional)

    private LocalDateTime timestamp;
    private String userEmail;
    String Status;
}
