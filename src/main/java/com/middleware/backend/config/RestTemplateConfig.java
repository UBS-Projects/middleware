package com.middleware.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Value("${camel.rest.host}")
    private String host;

    @Value("${camel.rest.port}")
    private int port;

    @Value("${camel.rest.context-path}")
    private String contextPath;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public String getBaseUrl() {
        return "http://" + host + ":" + port + contextPath;
    }

}
