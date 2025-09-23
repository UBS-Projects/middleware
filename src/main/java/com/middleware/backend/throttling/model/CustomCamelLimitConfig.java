package com.middleware.backend.throttling.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity storing per-route Camel rate limit configuration.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "custom_camel_rate_limit")
public class CustomCamelLimitConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** Window duration in seconds. */
    private int seconds;
    /** Maximum number of requests allowed within the window. */
    private int requestLimit;
    /** Identifier of the Camel route this configuration applies to. */
    private String routeId;
}
