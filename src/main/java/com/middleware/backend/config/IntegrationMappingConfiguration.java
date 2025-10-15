package com.middleware.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.IntegrationMappingComponent;
import com.middleware.service.IntegrationMappingService;
import com.middleware.model.MappingRequest;
import com.middleware.model.MappingResponse;
import com.middleware.backend.kaotocamel.dto.MiddlewareResponseDto;
import com.middleware.backend.kaotocamel.service.MiddlewareProcessorService;
import org.apache.camel.CamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Integration Mapping Component
 */
@Configuration
public class IntegrationMappingConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(IntegrationMappingConfiguration.class);

    @Autowired
    private MiddlewareProcessorService middlewareProcessorService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * ✅ Bean 1: IntegrationMappingService implementation (business logic)
     */
    @Bean(name = "integrationMappingComponentService")
    public IntegrationMappingService integrationMappingComponentServiceImplementation() {
        return new IntegrationMappingService() {

            @Override
            public MappingResponse processMapping(MappingRequest request) {
                try {
                    LOG.info("Processing mapping for API: {}", request.getMiddlewareApiName());

                    if (request.getMiddlewareApiName() == null || request.getMiddlewareApiName().trim().isEmpty()) {
                        return MappingResponse.error(400, "ERROR", "Middleware API name is required");
                    }

                    if (request.getDhis2Code() == null || request.getDhis2Code().trim().isEmpty()) {
                        return MappingResponse.error(400, "ERROR", "DHIS2 code (_dhis2Code) is required");
                    }

                    MiddlewareResponseDto result = middlewareProcessorService.processMiddlewareRequest(
                            request.getMiddlewareApiName(),
                            request.getPeriodParam(),
                            request.getOuParam(),
                            request.getDhis2Code()
                    );

                    String jsonData = objectMapper.writeValueAsString(result);
                    LOG.info("Mapping processed successfully for API: {} with {} rows",
                            request.getMiddlewareApiName(), result.getRows().size());

                    return MappingResponse.success("Mapping processed successfully", jsonData);

                } catch (Exception e) {
                    LOG.error("Failed to process mapping: {}", e.getMessage(), e);
                    return MappingResponse.error(500, "ERROR", "Failed to process mapping: " + e.getMessage());
                }
            }
        };
    }

    /**
     * ✅ Bean 2: Register the Camel Component itself
     */
    @Bean("integrationmapping")
    public IntegrationMappingComponent integrationMappingComponent(CamelContext camelContext,
                                                                   IntegrationMappingService integrationMappingComponentService) {
        IntegrationMappingComponent component = new IntegrationMappingComponent(camelContext);
        component.setIntegrationMappingService(integrationMappingComponentService);
        LOG.info("✅ Registered custom Camel component: integrationmapping");
        return component;
    }
}
