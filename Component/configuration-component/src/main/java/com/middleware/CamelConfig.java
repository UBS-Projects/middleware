package com.middleware;

import org.apache.camel.CamelContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CamelConfig {

    @Bean
    public ConfigComponent configComponent(CamelContext camelContext) {
        ConfigComponent component = new ConfigComponent();
        camelContext.addComponent("config", component);
        return component;
    }
}
