package com.middleware.backend.throttling.service;

import com.middleware.backend.throttling.model.RateLimitConfig;
import com.middleware.backend.throttling.repository.RateLimitConfigRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class RateLimitService {
    private final RateLimitConfigRepository repo;
    public RateLimitConfig getConfig() {
        return repo.findTopByOrderByIdDesc().orElseGet(() -> {
            // default values if not set in DB
            RateLimitConfig cfg = new RateLimitConfig(1L, 100, 60);
            repo.save(cfg);
            return cfg;
        });
    }
    public RateLimitConfig updateConfig(int limit, int windowSeconds) {
        RateLimitConfig cfg = new RateLimitConfig(1L, limit, windowSeconds);
        return repo.save(cfg);
    }
}