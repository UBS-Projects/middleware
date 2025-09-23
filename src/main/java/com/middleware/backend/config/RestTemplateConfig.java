package com.middleware.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class for creating and managing a {@link RestTemplate} bean.
 * This class also defines properties for configuring the base URL for REST calls.
 */
@Configuration
public class RestTemplateConfig {

    @Value("${camel.rest.host}")
    private String host;

    @Value("${camel.rest.port}")
    private int port;

    @Value("${camel.rest.context-path}")
    private String contextPath;

    /**
     * Creates a singleton {@link RestTemplate} bean.
     * This bean can be injected into other components to make HTTP requests.
     *
     * @return A new instance of {@link RestTemplate}.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Constructs and provides the base URL for the Camel REST services.
     * The URL is built from properties defined in the application's configuration.
     *
     * @return The base URL as a string.
     */
    @Bean
    public String getBaseUrl() {
        return "http://" + host + ":" + port + contextPath;
    }

}
