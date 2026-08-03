package com.middleware.backend.camel.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Audit record for actions performed on dynamic routes.
 * Each entry captures a mutation or lifecycle event (e.g., upload, start, stop, revert, deactivate)
 * associated with a specific route version, along with who performed it and when.
 */
@Entity
@Table(name = "dynamic_route_audit")
@Data
@NoArgsConstructor
public class DynamicRouteAudit {
    /** Unique identifier of the audit record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The stable route identifier that this audit entry relates to. */
    private String routeId;

    /** The version of the route at the time the action occurred. */
    private int version;

    /** The performed action, e.g., "upload", "start", "stop", "revert", or "deactivate". */
    private String action; // upload, start, stop, revert, deactivate

    /** Additional free-form details about the action or context. */
    private String details; // Additional info (optional)

    /** Timestamp of when the action was recorded. */
    private LocalDateTime timestamp;

    /** Email (or identifier) of the user who performed the action. */
    private String userEmail;

    /** Resulting status after the action, if applicable. */
    private String Status;
}
