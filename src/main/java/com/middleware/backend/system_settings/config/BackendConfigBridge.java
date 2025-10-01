package com.middleware.backend.system_settings.config;

import com.middleware.model.ConfigDetail;
import com.middleware.service.ConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Slf4j
@Configuration
public class BackendConfigBridge {

    private final com.middleware.backend.system_settings.service.ConfigService backendConfigService;

    public BackendConfigBridge(com.middleware.backend.system_settings.service.ConfigService backendConfigService) {
        this.backendConfigService = backendConfigService;
    }

    @Bean(name = "configServiceBridge")
    public ConfigService configServiceImplementation() {
        return new ConfigService() {
            @Override
            public ConfigDetail getConfigs(String code) {
                Map<String, String> backendConfigs = backendConfigService.getModuleConfig(code);
                ConfigDetail detail = new ConfigDetail();
                detail.setCode(code);
                detail.setConfigs(backendConfigs);
                log.info("Returning config for code={}: {}", code, backendConfigs);
                return detail;
            }
        };
    }
}
