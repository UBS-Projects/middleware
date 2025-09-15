package com.middleware.backend.throttling.repository;

import com.middleware.backend.throttling.model.CamelLimitConfig;
import com.middleware.backend.throttling.model.RateLimitConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CamelRateLimitRepository extends JpaRepository<CamelLimitConfig,Long> {
    Optional<CamelLimitConfig> findTopByOrderByIdDesc();
}
