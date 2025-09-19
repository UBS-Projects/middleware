package com.middleware.backend.throttling.repository;

import com.middleware.backend.throttling.model.CamelLimitConfig;
import com.middleware.backend.throttling.model.CustomCamelLimitConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for {@link com.middleware.backend.throttling.model.CustomCamelLimitConfig} entities.
 */
public interface CustomCamelRateLimitRepository extends JpaRepository<CustomCamelLimitConfig,Long> {
    /**
     * Find a per-route configuration by route id.
     * @param routeId identifier of the Camel route
     * @return optional containing the configuration when present
     */
    Optional<CustomCamelLimitConfig> findByRouteId(String routeId);
}
