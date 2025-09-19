package com.middleware.backend.system_settings.config;

import com.middleware.backend.system_settings.model.Dhis2;
import com.middleware.backend.system_settings.service.Dhis2Service;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Application-scoped holder for the active DHIS2 settings.
 * <p>
 * Loads configuration from the database on startup and exposes a simple in-memory cache.
 */
@Component
@RequiredArgsConstructor
@Getter
public class Dhis2Config {

    private final Dhis2Service dhis2Service;
    private Dhis2 currentSettings;

    /**
     * Initializes the in-memory settings cache from the latest persisted record.
     */
    @PostConstruct
    public void loadSettings() {
        this.currentSettings = dhis2Service.getActiveSettings();
    }

    /**
     * Refreshes the cached settings, typically called after updates.
     */
    public void refreshSettings() {
        this.currentSettings = dhis2Service.getActiveSettings();
    }
}
