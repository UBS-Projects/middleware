package com.middleware.backend.integrated_systems.config;

import com.middleware.backend.integrated_systems.dto.IntegratedSystemDto;
import com.middleware.backend.integrated_systems.model.Protocol;
import com.middleware.backend.integrated_systems.service.IntegratedSystemService;
import com.middleware.model.IntegratedSystemDetail;
import com.middleware.service.IntegratedSystemBridgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration bridge that connects the backend IntegratedSystemService
 * with the middleware's {@link IntegratedSystemBridgeService}.
 * <p>
 * This bridge provides system configuration details (host, port, authentication, etc.)
 * for any integrated system by its unique code.
 */
@Slf4j
@Configuration
public class IntegratedSystemBridge {

    private final IntegratedSystemService backendService;

    public IntegratedSystemBridge(IntegratedSystemService backendService) {
        this.backendService = backendService;
    }

    /**
     * Creates a bean of {@link IntegratedSystemBridgeService} to expose system configuration details.
     *
     * @return an implementation of IntegratedSystemBridgeService that retrieves system configurations
     *         based on their unique system code.
     */
    @Bean(name = "integratedSystemServiceBridge")
    public IntegratedSystemBridgeService integratedSystemBridgeService() {
        return code -> {
            try {
                IntegratedSystemDto dto = backendService.getById(code);

                if (dto == null) {
                    log.warn("No IntegratedSystem found for code={}", code);
                    return null;
                }

                Map<String, Object> map = new LinkedHashMap<>();
                map.put("host", dto.getHost());
                map.put("port", resolvePort(dto));
                // Store enum names as strings so JDBC / HTTP consumers can match reliably
                map.put("systemType", dto.getSystemType() != null ? dto.getSystemType().name() : null);
                map.put("protocol", dto.getProtocol() != null ? dto.getProtocol().name() : null);
                map.put(
                        "authenticationType",
                        dto.getAuthenticationType() != null ? dto.getAuthenticationType().name() : null);
                map.put("username", dto.getUsername());
                map.put("password", dto.getPassword());
                map.put("token", dto.getToken());

                // Add optional keys if present
                if (StringUtils.hasText(dto.getAdditionalKey1())) {
                    map.put(dto.getAdditionalKey1(), dto.getAdditionalValue1());
                }
                if (StringUtils.hasText(dto.getAdditionalKey2())) {
                    map.put(dto.getAdditionalKey2(), dto.getAdditionalValue2());
                }

                IntegratedSystemDetail detail = new IntegratedSystemDetail();
                detail.setCode(dto.getCode());
                detail.setConfig(Collections.unmodifiableMap(map));

                log.info("Loaded IntegratedSystem config for code={}: {}", code, map);
                return detail;

            } catch (RuntimeException e) {
                log.error("Error retrieving IntegratedSystem for code={}: {}", code, e.getMessage(), e);
                return null;
            }
        };
    }

    /**
     * Uses the configured port when present; otherwise the protocol default
     * (80/443 for HTTP/HTTPS so existing API systems keep working).
     */
    private static Object resolvePort(IntegratedSystemDto dto) {
        if (StringUtils.hasText(dto.getPort())) {
            return dto.getPort();
        }
        Protocol protocol = dto.getProtocol();
        if (protocol == null) {
            return 443;
        }
        return protocol.defaultPort();
    }
}
