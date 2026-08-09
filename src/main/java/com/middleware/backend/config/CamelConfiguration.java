package com.middleware.backend.config;

import com.middleware.backend.errormapping.service.GlobalErrorMappingRoutePolicyFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.spi.RoutePolicyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class CamelConfiguration {

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private RoutePolicyFactory loggingRoutePolicyFactory;

    @Autowired
    private GlobalErrorMappingRoutePolicyFactory globalErrorMappingRoutePolicyFactory;

    @PostConstruct
    public void configureCamel() {
        camelContext.addRoutePolicyFactory(loggingRoutePolicyFactory);
        camelContext.addRoutePolicyFactory(globalErrorMappingRoutePolicyFactory);
    }
}
