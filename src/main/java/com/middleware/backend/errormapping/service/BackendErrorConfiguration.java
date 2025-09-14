package com.middleware.backend.errormapping.service;

import com.middleware.component.errormapper.model.ErrorMappingDetail;
import com.middleware.component.errormapper.service.ErrorMappingService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for providing the ErrorMappingService implementation.
 * This class defines the error mapping rules that the custom Camel component
 * will use.
 */
@Slf4j
@Configuration
public class BackendErrorConfiguration {

    // private static final Logger LOG =
    // LoggerFactory.getLogger(BackendErrorConfiguration.class);

    // This is the new service you want to use for managing and retrieving error
    // mappings.

    @Autowired
    private BackendErrorMappingService backendErrorMappingService;

    @Bean(name = ErrorMappingService.BEAN_ID) // Important: Register with the predefined BEAN_ID
    public ErrorMappingService ErrorMappingServiceImplementation() {
        return new ErrorMappingService() {

            @Override
            public ErrorMappingDetail findMatchingErrorMapping(String routeId, Long sourceSystemId,
                    String errorMessage) {

                return backendErrorMappingService.findMatchingErrorMapping(routeId, sourceSystemId, errorMessage)
                        .map(dto -> {
                            ErrorMappingDetail detail = new ErrorMappingDetail();
                            detail.setMappedErrorCode(dto.getMappedErrorCode());
                            detail.setMappedMessage(dto.getMappedMessage());
                            detail.setHttpStatusCode(dto.getHttpStatusCode());
                            // Additional fields from DTO to Detail
                            detail.setRouteId(dto.getRouteId());
                            detail.setRoutePath(dto.getRoutePath());
                            detail.setRawErrorSubstring(dto.getRawErrorSubstring());
                            // Ensure this is not null before converting
                            if (dto.getMatchType() != null) {
                                detail.setMatchType(dto.getMatchType().name());
                            }
                            detail.setSourceSystem(dto.getSourceSystemName());
                            detail.setErrorCategory(dto.getErrorCategoryName());
                            detail.setLanguage(dto.getLanguage());
                            detail.setActive(dto.getActive());
                            return detail;
                        }).orElse(null);
            }

        };
    }
}
