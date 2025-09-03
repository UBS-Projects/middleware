package com.middleware.backend.throttling.controller;

import com.middleware.backend.throttling.model.RateLimitConfig;
import com.middleware.backend.throttling.service.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/rate-limit")
@RequiredArgsConstructor
public class RateLimitAdminController {

    private final RateLimitService service;

    @GetMapping
    @PreAuthorize("hasAuthority('throttling:view')")
    @Operation(
            summary = "Retrieve current rate limit configuration",
            description = "Returns the currently configured rate limit settings. Requires authority 'throttling:view'."
    )
    public RateLimitConfig getConfig() {
        return service.getConfig();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('throttling:edit')")
    @Operation(
            summary = "Update rate limit configuration",
            description = "Updates the system-wide rate limit configuration with the provided values. " +
                    "Parameters: 'limit' is the maximum number of requests allowed, " +
                    "'windowSeconds' is the time window in seconds. " +
                    "Requires authority 'throttling:edit'."
    )
    public RateLimitConfig update(@RequestParam int limit, @RequestParam int windowSeconds) {
        return service.updateConfig(limit, windowSeconds);
    }
}