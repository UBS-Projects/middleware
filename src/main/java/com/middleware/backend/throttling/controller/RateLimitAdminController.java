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



//************************ Camel Rate Limit ****************************************************************
@GetMapping("/camel")
@PreAuthorize("hasAuthority('throttling:view')")
@Operation(
        summary = "Retrieve current Camel Routes rate limit configuration",
        description = "Returns the currently configured Camel Routes rate limit settings. Requires authority 'throttling:view'."
)
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
    public ResponseEntity<?> deleteCustomCamelLimit(@RequestParam String routeId) {
        return service.deleteConfig(routeId);
    }
}