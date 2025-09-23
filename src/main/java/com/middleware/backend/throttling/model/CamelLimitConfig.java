package com.middleware.backend.throttling.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity storing global Camel routes rate limit configuration.
 * <p>
 * Controls how many Camel requests are allowed within a given time window.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "camel_rate_limit")
public class CamelLimitConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * Window duration in seconds.
     */
    private int seconds;
    /**
     * Maximum number of requests allowed within the window.
     */
    private int requestLimit;
}
