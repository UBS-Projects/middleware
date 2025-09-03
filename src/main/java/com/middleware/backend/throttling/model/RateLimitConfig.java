package com.middleware.backend.throttling.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rate_limit_config")
public class RateLimitConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // single row for global config

    private int limitRequests;          // e.g., 100
    private int windowSeconds;  // e.g., 60
}