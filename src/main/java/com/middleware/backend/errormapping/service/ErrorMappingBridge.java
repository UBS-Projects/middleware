package com.middleware.backend.errormapping.service;

import com.middleware.component.model.ErrorMappingDetail;
import com.middleware.component.service.ErrorMappingBridgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bridge between backend ErrorMappingService and the middleware Camel component.
 * Returns full mapping info (including substring & match type) so the component can decide
 * whether to apply the mapping based on the raw error message.
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
        return code -> {
            try {
                return backendService.getErrorMappingByCode(code)
                        .map(dto -> ErrorMappingDetail.builder()
                                .id(dto.getId())
                                .routeId(dto.getRouteId())
                                .mappedErrorCode(dto.getMappedErrorCode())
                                .mappedMessage(dto.getMappedMessage())
                                .httpStatusCode(dto.getHttpStatusCode())
                                .language(dto.getLanguage())
                                .active(dto.getActive())
                                .rawErrorSubstring(dto.getRawErrorSubstring())
                                .matchType(dto.getMatchType() != null
                                        ? dto.getMatchType().name()
                                        : null)

                                .build())
                        .orElse(null);
            } catch (Exception e) {
                log.error("Error fetching ErrorMapping by code {}: {}", code, e.getMessage());
                return null;
            }
        };
    }
}
