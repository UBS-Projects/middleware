package com.middleware.backend.config;

import org.apache.camel.dsl.yaml.YamlRoutesBuilderLoader;
import org.apache.camel.spi.RoutesBuilderLoader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CamelYamlLoaderConfig {

    @Bean
    @Qualifier("yaml")
    public RoutesBuilderLoader yamlRoutesBuilderLoader() {
        return new YamlRoutesBuilderLoader();
    }

}
