package com.middleware.backend.throttling.controller;

import com.middleware.backend.throttling.model.CamelLimitConfig;
import com.middleware.backend.throttling.model.CustomCamelLimitConfig;
import com.middleware.backend.throttling.model.RateLimitConfig;
import com.middleware.backend.throttling.service.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Administrative REST API for managing rate limit configurations.
 * <p>
 * Secured via method-level {@link org.springframework.security.access.prepost.PreAuthorize} annotations.
 * Provides CRUD-style operations for system-wide limits, Camel global limits, and per-route Camel limits.
 * </p>
 */
@RestController
@RequestMapping("/api/admin/rate-limit")
@RequiredArgsConstructor
public class RateLimitAdminController {

    private final RateLimitService service;

//************************ System Rate Limit ****************************************************************
    @GetMapping
    @PreAuthorize("hasAuthority('throttling:view')")
    @Operation(
            summary = "Retrieve current rate limit configuration",
            description = "Returns the currently configured rate limit settings. Requires authority 'throttling:view'."
    )
    /**
     * Retrieve current system-wide rate limit configuration.
     * @return the persisted or default {@link RateLimitConfig}
     */
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
    /**
     * Update system-wide rate limit configuration.
     * @param limit maximum allowed requests within the window
     * @param windowSeconds length of the window in seconds
     * @return the saved {@link RateLimitConfig}
     */
    public RateLimitConfig update(@RequestParam int limit, @RequestParam int windowSeconds) {
        return service.updateConfig(limit, windowSeconds);
    }



//************************ Camel Rate Limit ****************************************************************
@GetMapping("/camel")
@PreAuthorize("hasAuthority('throttling:view')")
@Operation(
        summary = "Retrieve current Camel Routes rate limit configuration",
        description = "Returns the currently configured Camel Routes rate limit settings. Requires authority 'throttling:view'."
)
/**
 * Retrieve current global Camel routes rate limit configuration.
 * @return the persisted {@link CamelLimitConfig}
 */
public CamelLimitConfig getCamelConfig() {
    return service.getCamelConfig();
}

    @PostMapping("/camel")
    @PreAuthorize("hasAuthority('throttling:edit')")
    @Operation(
            summary = "Update Camel Routes rate limit configuration",
            description = "Updates the Camel Routes rate limit configuration with the provided values. " +
                    "Parameters: 'limit' is the maximum number of Camel requests allowed, " +
                    "'windowSeconds' is the time window in seconds. " +
                    "Requires authority 'throttling:edit'."
    )
    /**
     * Update global Camel routes rate limit configuration.
     * @param limit maximum Camel requests within the window
     * @param windowSeconds window duration in seconds
     * @return saved {@link CamelLimitConfig}
     */
    public CamelLimitConfig updateCamelLimit(@RequestParam int limit, @RequestParam int windowSeconds) {
        return service.updateCamelConfig(limit, windowSeconds);
    }


    //************************ Custom Camel Rate Limit ****************************************************************
    @GetMapping("/camel/custom")
    @PreAuthorize("hasAuthority('throttling:view')")
    @Operation(
            summary = "Retrieve custom Camel Routes rate limit configuration",
            description = "Returns list of custom Camel Routes rate limit settings. Requires authority 'throttling:view'."
    )
    /**
     * List all custom Camel per-route rate limit configurations.
     * @return HTTP 200 with list of {@link CustomCamelLimitConfig}
     */
    public ResponseEntity<?> getCustomCamelConfig() {
        return service.getCustomCamelConfig();
    }

    @PostMapping("/camel/custom")
    @PreAuthorize("hasAuthority('throttling:edit')")
    @Operation(
            summary = "Update a custom Camel Route rate limit configuration",
            description = "Updates a custom Camel Route rate limit configuration with the provided values. " +
                    "Parameters: 'limit' is the maximum number of Camel requests allowed, " +
                    "'windowSeconds' is the time window in seconds, " +
                    "'routeId' is the route id to edit. " +
                    "Requires authority 'throttling:edit'.")
    /**
     * Create or update a custom per-route Camel rate limit configuration.
     * @param limit maximum requests for the route within the window
     * @param windowSeconds window duration in seconds
     * @param routeId route identifier to configure
     * @return HTTP 200 with saved {@link CustomCamelLimitConfig}
     */
    public ResponseEntity<?> updateCustomCamelLimit(@RequestParam int limit, @RequestParam int windowSeconds, @RequestParam String routeId) {
        return service.updateCustomCamelConfig(limit,windowSeconds,routeId);
    }


    @DeleteMapping("/camel/custom")
    @PreAuthorize("hasAuthority('throttling:edit')")
    @Operation(
            summary = "Remove a custom Camel Route rate limit configuration",
            description = "Removes a custom Camel Route rate limit configuration with the provided value. " +
                    "Parameters: 'routeId' is the route id to Delete. " +
                    "Requires authority 'throttling:edit'.")
    /**
     * Delete a custom per-route Camel rate limit configuration.
     * @param routeId the route identifier to remove configuration for
     * @return HTTP 200 when deleted, 404 when not found
     */
    public ResponseEntity<?> deleteCustomCamelLimit(@RequestParam String routeId) {
        return service.deleteConfig(routeId);
    }
}