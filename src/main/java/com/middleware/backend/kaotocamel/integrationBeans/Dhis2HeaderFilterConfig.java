package com.middleware.backend.kaotocamel.integrationBeans;
// package com.middleware.backend.kaotocamel.config;
import org.apache.camel.spi.HeaderFilterStrategy;
import org.apache.camel.support.DefaultHeaderFilterStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Dhis2HeaderFilterConfig {
    @Bean("dhis2HeaderFilter")
    public HeaderFilterStrategy dhis2HeaderFilter() {
        DefaultHeaderFilterStrategy f = new DefaultHeaderFilterStrategy();
        f.setCaseInsensitive(true);

         f.getOutFilter().add("authorization");
        f.getOutFilter().add("proxy-authorization");
        f.getOutFilter().add("cookie");
        f.getOutFilter().add("x-*");

         f.getOutFilter().add("content-length");
        f.getOutFilter().add("transfer-encoding");
        f.getOutFilter().add("host");
        f.getOutFilter().add("accept-encoding");

        return f;
    }
}
