package com.middleware.backend.audit_logs_interceptor.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Intercepts incoming HTTP requests to log basic information about them.
 * This interceptor is registered with Spring's {@link org.springframework.web.servlet.config.annotation.InterceptorRegistry}
 * and is invoked for each request that matches its configured path patterns.
 */
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    /**
     * Logs the method and URI of the incoming request before it is handled by the controller.
     *
     * @param request  The incoming HTTP request.
     * @param response The HTTP response.
     * @param handler  The handler (e.g., a controller method) that will process the request.
     * @return {@code true} to continue processing the request; {@code false} to stop the execution chain.
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        return true; // continue processing
    }
}
