package com.middleware.backend.system_settings.controller;

import com.middleware.backend.system_settings.config.Dhis2Config;
import com.middleware.backend.system_settings.dto.Dhis2Dto;
import com.middleware.backend.system_settings.service.Dhis2Service;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dhis2")
@AllArgsConstructor
public class Dhis2Controller {

    private final Dhis2Service service;
    private final Dhis2Config config;

    @GetMapping
    @PreAuthorize("hasAuthority('dhis2:view')")
    @Operation(
            summary = "Retrieve current DHIS2 configuration",
            description = "Returns the currently stored DHIS2 settings. " +
                    "Useful for displaying the configuration in admin UI or other monitoring purposes. Requires authority 'dhis2:view'."
    )
    public ResponseEntity<?> getSettings() {
        return service.getSettings();
    }

    @PatchMapping
    @PreAuthorize("hasAuthority('dhis2:edit')")
    @Operation(
            summary = "Update DHIS2 configuration",
            description = "Updates the DHIS2 settings with the provided values. " +
                    "After update, the in-memory configuration cache is refreshed automatically. Requires authority 'dhis2:edit'."
    )
    public ResponseEntity<?> updateSettings(@RequestBody Dhis2Dto dhis2) {
        ResponseEntity<?> response = service.save(dhis2);
        config.refreshSettings(); // refresh cache here, outside of service
        return response;
    }
}
