package com.middleware.backend.throttling.repository;

import com.middleware.backend.throttling.model.CamelLimitConfig;
import com.middleware.backend.throttling.model.CustomCamelLimitConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomCamelRateLimitRepository extends JpaRepository<CustomCamelLimitConfig,Long> {
    Optional<CustomCamelLimitConfig> findByRouteId(String routeId);
}
