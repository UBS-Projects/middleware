package com.middleware.backend.system_settings.config;

import com.middleware.backend.system_settings.model.Dhis2;
import com.middleware.backend.system_settings.service.Dhis2Service;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Getter
public class Dhis2Config {

    private final Dhis2Service dhis2Service;
    private Dhis2 currentSettings;

    @PostConstruct
    public void loadSettings() {
        this.currentSettings = dhis2Service.getActiveSettings();
    }

    public void refreshSettings() {
        this.currentSettings = dhis2Service.getActiveSettings();
    }
}
