package com.middleware.backend.audit_logs_interceptor.config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configures web-related beans for the Spring application, including interceptors.
 * This class implements {@link WebMvcConfigurer} to customize the default Spring MVC configuration.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private RequestLoggingInterceptor requestLoggingInterceptor;

    /**
     * Adds the {@link RequestLoggingInterceptor} to the application's interceptor registry.
     * This ensures that the interceptor is applied to incoming requests, allowing it to log request details.
     *
     * @param registry The interceptor registry to which the new interceptor will be added.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestLoggingInterceptor);
    }
}