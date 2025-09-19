package com.middleware.backend.throttling.repository;

import com.middleware.backend.throttling.model.RateLimitConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for reading and writing {@link com.middleware.backend.throttling.model.RateLimitConfig} entities.
 */
@Repository
public interface RateLimitConfigRepository extends JpaRepository<RateLimitConfig, Long> {
    /**
     * Return the most recent configuration entry.
     * @return latest config by id or empty when none
     */
    Optional<RateLimitConfig> findTopByOrderByIdDesc();
}