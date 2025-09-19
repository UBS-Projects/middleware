package com.middleware.backend.audit_logs_interceptor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

/**
 * Configures request logging for the application.
 * This class sets up a filter to log incoming request details, which is useful for debugging and auditing purposes.
 */
@Configuration
public class LoggingConfig {

    /**
     * Creates and configures a {@link CommonsRequestLoggingFilter} bean.
     * The filter is configured to include client information, query strings, and the request payload in the logs.
     * The maximum payload length to be logged is set to 1000 characters.
     *
     * @return A configured {@link CommonsRequestLoggingFilter} instance.
     */
    @Bean
    public CommonsRequestLoggingFilter logFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeClientInfo(true);
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(1000);
        return filter;
    }
}
