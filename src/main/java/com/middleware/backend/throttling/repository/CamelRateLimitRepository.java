package com.middleware.backend.throttling.repository;

import com.middleware.backend.throttling.model.CamelLimitConfig;
import com.middleware.backend.throttling.model.RateLimitConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for {@link com.middleware.backend.throttling.model.CamelLimitConfig} entities.
 */
public interface CamelRateLimitRepository extends JpaRepository<CamelLimitConfig,Long> {
    /**
     * Return the most recent Camel rate limit configuration.
     * @return optional latest config
     */
    Optional<CamelLimitConfig> findTopByOrderByIdDesc();
}
