package com.middleware.backend.kaotocamel.integrationBeans;

import org.apache.camel.spi.HeaderFilterStrategy;
import org.apache.camel.support.DefaultHeaderFilterStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class Dhis2HeaderFilterConfig {

    private static final Logger log = LoggerFactory.getLogger(Dhis2HeaderFilterConfig.class);

    @Bean("dhis2HeaderFilter")
    public HeaderFilterStrategy dhis2HeaderFilter() {
        log.info("=== Configuring DHIS2 Header Filter (Static Test) ===");

        DefaultHeaderFilterStrategy filter = new DefaultHeaderFilterStrategy();
        filter.setCaseInsensitive(true);

        filter.getOutFilter().add("cookie");
        filter.getOutFilter().add("set-cookie");

        log.info("DHIS2 Header Filter configured - allowing most headers for testing");

        return filter;
    }
}
