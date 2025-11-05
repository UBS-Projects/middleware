package com.middleware.backend.errormapping.service;

import com.middleware.component.errormapper.model.ErrorMappingDetail;
import com.middleware.component.errormapper.service.ErrorMappingBridgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bridge between backend ErrorMappingService and the middleware Camel component.
 */
@Slf4j
@Configuration
public class ErrorMappingBridge {

    private final BackendErrorMappingService backendService;

    public ErrorMappingBridge(BackendErrorMappingService backendService) {
        this.backendService = backendService;
    }

    @Bean(name = ErrorMappingBridgeService.BEAN_ID)
    public ErrorMappingBridgeService errorMappingBridgeService() {
        return id -> {
            try {
                return backendService.getErrorMappingById(id)
                        .map(dto -> ErrorMappingDetail.builder()
                                .id(dto.getId())
                                .routeId(dto.getRouteId())
                                .mappedErrorCode(dto.getMappedErrorCode())
                                .mappedMessage(dto.getMappedMessage())
                                .httpStatusCode(dto.getHttpStatusCode())
                                .language(dto.getLanguage())
                                .active(dto.getActive())
                                .build())
                        .orElse(null);
            } catch (Exception e) {
                log.error("Error fetching ErrorMapping by ID {}: {}", id, e.getMessage());
                return null;
            }
        };
    }
}