package com.middleware.backend.config;

import org.apache.camel.dsl.yaml.YamlRoutesBuilderLoader;
import org.apache.camel.spi.RoutesBuilderLoader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for setting up a YAML-based route loader for Apache Camel.
 * This allows Camel routes to be defined in YAML files instead of Java DSL.
 */
@Configuration
public class CamelYamlLoaderConfig {

    /**
     * Creates a bean for the YAML routes builder loader.
     * This loader enables Apache Camel to discover and load routes defined in YAML format.
     * The bean is qualified with the name "yaml" to allow for specific injection if needed.
     *
     * @return An instance of {@link RoutesBuilderLoader} configured for YAML.
     */
    @Bean
    @Qualifier("yaml")
    public RoutesBuilderLoader yamlRoutesBuilderLoader() {
        return new YamlRoutesBuilderLoader();
    }

}
