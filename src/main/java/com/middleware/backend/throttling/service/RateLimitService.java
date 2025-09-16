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

@Service
@AllArgsConstructor
public class RateLimitService {
    private final RateLimitConfigRepository repo;
    private final CamelRateLimitRepository camelRepo;
    private final CustomCamelRateLimitRepository customRepo;
    //************************ System Rate Limit ****************************************************************
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

    //************************ Camel Rate Limit ****************************************************************
    public CamelLimitConfig getCamelConfig() {
        return camelRepo.findTopByOrderByIdDesc().get();
    }

    public CamelLimitConfig updateCamelConfig(int limit, int windowSeconds) {
        CamelLimitConfig cfg = new CamelLimitConfig(1L, windowSeconds,limit);
        return camelRepo.save(cfg);
    }

    //************************ Custom Camel Rate Limit ****************************************************************

    public Optional<CustomCamelLimitConfig> findByRouteId(String routeID) {
        return customRepo.findByRouteId(routeID);
    }

    public ResponseEntity<?> getCustomCamelConfig() {
        return ResponseEntity.ok(customRepo.findAll());
    }
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
    public ResponseEntity<?> deleteConfig(String routeId){
        Optional<CustomCamelLimitConfig> exists = customRepo.findByRouteId(routeId);
        if(exists.isPresent()){
            customRepo.deleteById(exists.get().getId());
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }


}