package com.middleware.backend.config;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.RoutePolicyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Configures the Camel context for the application.
 * This class is responsible for setting up and customizing the Apache Camel framework,
 * including the registration of route policies.
 */
@Configuration
public class CamelConfiguration {

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private RoutePolicyFactory loggingRoutePolicyFactory;

    /**
     * Configures the Camel context after the bean has been initialized.
     * This method adds a custom {@link RoutePolicyFactory} to the Camel context,
     * which in this case is used for logging purposes across all routes.
     */
    @PostConstruct
    public void configureCamel() {
        camelContext.addRoutePolicyFactory(loggingRoutePolicyFactory);
    }
}
