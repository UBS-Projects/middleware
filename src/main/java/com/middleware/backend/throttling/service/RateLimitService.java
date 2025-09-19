package com.middleware.backend.throttling.service;

import com.middleware.backend.throttling.model.CamelLimitConfig;
import com.middleware.backend.throttling.model.CustomCamelLimitConfig;
import com.middleware.backend.throttling.model.RateLimitConfig;
import com.middleware.backend.throttling.repository.CamelRateLimitRepository;
import com.middleware.backend.throttling.repository.CustomCamelRateLimitRepository;
import com.middleware.backend.throttling.repository.RateLimitConfigRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for managing rate limit configurations used by request filters and admin API.
 */
@Service
@AllArgsConstructor
public class RateLimitService {
    private final RateLimitConfigRepository repo;
    private final CamelRateLimitRepository camelRepo;
    private final CustomCamelRateLimitRepository customRepo;
    //************************ System Rate Limit ****************************************************************
    /**
     * Get current system-wide rate limit config, creating a default if not present.
     * @return existing or default {@link RateLimitConfig}
     */
    public RateLimitConfig getConfig() {
        return repo.findTopByOrderByIdDesc().orElseGet(() -> {
            // default values if not set in DB
            RateLimitConfig cfg = new RateLimitConfig(1L, 100, 60);
            repo.save(cfg);
            return cfg;
        });
    }
    /**
     * Update system-wide rate limit config.
     * @param limit maximum allowed requests in window
     * @param windowSeconds window duration in seconds
     * @return saved {@link RateLimitConfig}
     */
    public RateLimitConfig updateConfig(int limit, int windowSeconds) {
        RateLimitConfig cfg = new RateLimitConfig(1L, limit, windowSeconds);
        return repo.save(cfg);
    }

    //************************ Camel Rate Limit ****************************************************************
    /**
     * Get current global Camel routes rate limit configuration.
     * @return existing {@link CamelLimitConfig}
     */
    public CamelLimitConfig getCamelConfig() {
        return camelRepo.findTopByOrderByIdDesc().get();
    }

    /**
     * Update (upsert) global Camel routes rate limit configuration.
     * @param limit maximum Camel requests in window
     * @param windowSeconds window duration in seconds
     * @return saved {@link CamelLimitConfig}
     */
    public CamelLimitConfig updateCamelConfig(int limit, int windowSeconds) {
        CamelLimitConfig cfg = new CamelLimitConfig(1L, windowSeconds,limit);
        return camelRepo.save(cfg);
    }

    //************************ Custom Camel Rate Limit ****************************************************************

    /**
     * Find a custom per-route Camel rate limit by route id.
     * @param routeID route identifier
     * @return optional configuration
     */
    public Optional<CustomCamelLimitConfig> findByRouteId(String routeID) {
        return customRepo.findByRouteId(routeID);
    }

    /**
     * List all custom per-route Camel rate limits.
     * @return HTTP 200 with list of {@link CustomCamelLimitConfig}
     */
    public ResponseEntity<?> getCustomCamelConfig() {
        return ResponseEntity.ok(customRepo.findAll());
    }
    /**
     * Create or update custom per-route Camel limit.
     * @param limit maximum requests
     * @param windowSeconds window duration
     * @param routeId target route identifier
     * @return HTTP 200 with saved entity
     */
    public ResponseEntity<?> updateCustomCamelConfig(int limit, int windowSeconds, String routeId) {
        Optional<CustomCamelLimitConfig> exists = customRepo.findByRouteId(routeId);
        if(exists.isPresent()){
            exists.get().setRequestLimit(limit);
            exists.get().setSeconds(windowSeconds);
            return ResponseEntity.ok(customRepo.save(exists.get()));
        }
        CustomCamelLimitConfig config = CustomCamelLimitConfig.builder()
                .requestLimit(limit)
                .seconds(windowSeconds)
                .routeId(routeId)
                .build();

        return ResponseEntity.ok(customRepo.save(config));
    }
    /**
     * Delete a custom per-route Camel limit by route id.
     * @param routeId route identifier
     * @return HTTP 200 when deleted, 404 when not found
     */
    public ResponseEntity<?> deleteConfig(String routeId){
        Optional<CustomCamelLimitConfig> exists = customRepo.findByRouteId(routeId);
        if(exists.isPresent()){
            customRepo.deleteById(exists.get().getId());
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }


}