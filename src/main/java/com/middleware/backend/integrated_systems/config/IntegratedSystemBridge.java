package com.middleware.backend.integrated_systems.config;

import com.middleware.backend.integrated_systems.dto.IntegratedSystemDto;
import com.middleware.backend.integrated_systems.service.IntegratedSystemService;
import com.middleware.model.IntegratedSystemDetail;
import com.middleware.service.IntegratedSystemBridgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Configuration
public class IntegratedSystemBridge {

    private final IntegratedSystemService backendService;

    public IntegratedSystemBridge(IntegratedSystemService backendService) {
        this.backendService = backendService;
    }

    @Bean(name = "integratedSystemServiceBridge")
    public IntegratedSystemBridgeService integratedSystemBridgeService() {
        return new IntegratedSystemBridgeService() {
            @Override
            public IntegratedSystemDetail getSystemConfig(String code) {
                IntegratedSystemDto dto = backendService.getById(code);
                if (dto == null) {
                    log.warn("No IntegratedSystem found for code={}", code);
                    return null;
                }

                Map<String, Object> map = new LinkedHashMap<>();
                map.put("host", dto.getHost());
                map.put("port", dto.getPort());
                map.put("protocol", dto.getProtocol());
                map.put("authenticationType", dto.getAuthenticationType());
                map.put("username", dto.getUsername());
                map.put("password", dto.getPassword());
                map.put("token", dto.getToken());
                map.put(dto.getAdditionalKey1(), dto.getAdditionalValue1());
                map.put(dto.getAdditionalKey2(), dto.getAdditionalValue2());

                IntegratedSystemDetail detail = new IntegratedSystemDetail();
                detail.setCode(dto.getCode());
                detail.setConfig(map);

                log.info("Returning IntegratedSystem config for code={}: {}", code, map);
                return detail;
            }
        };
    }
}
