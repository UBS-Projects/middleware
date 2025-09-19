package com.middleware.backend.throttling.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity storing system-wide HTTP rate limit configuration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rate_limit_config")
public class RateLimitConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // single row for global config

    /** Maximum number of HTTP requests allowed within the window. */
    private int limitRequests;          // e.g., 100
    /** Window duration in seconds. */
    private int windowSeconds;  // e.g., 60
}