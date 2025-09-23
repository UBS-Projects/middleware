package com.middleware.backend.audit_logs_interceptor.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.backend.audit_logs_interceptor.model.AuditLog;
import com.middleware.backend.audit_logs_interceptor.repository.AuditLogRepository;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
/**
 * A servlet filter that intercepts incoming HTTP requests and outgoing responses to log them for auditing purposes.
 * This filter captures details such as the request method, path, headers, body, response status, and timing.
 * It excludes certain paths from logging, such as those related to audit logs themselves, Swagger UI, and API docs.
 */
@Component
@AllArgsConstructor
public class RequestLoggingFilter implements Filter {
    private final AuditLogRepository repo;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());

    /**
     * Processes the incoming request and outgoing response to log audit details.
     * This method wraps the request and response to allow their bodies to be read multiple times.
     * It logs the request and response details after the request has been processed by the filter chain.
     *
     * @param request  The incoming servlet request.
     * @param response The outgoing servlet response.
     * @param chain    The filter chain.
     * @throws IOException      if an I/O error occurs during the filtering process.
     * @throws ServletException if a servlet-related error occurs.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpRes = (HttpServletResponse) response;

        String path = httpReq.getRequestURI();

        if (path.startsWith("/api/auditlogs") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs") || path.startsWith("/camel")) {
            chain.doFilter(request, response);
            return;
        }
        // Wrap request/response to cache body
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpReq);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(httpRes);

        Instant start = Instant.now();

        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            Instant end = Instant.now();
            long duration = end.toEpochMilli() - start.toEpochMilli();

            logDetails(wrappedRequest, wrappedResponse, start, end, duration);

            // IMPORTANT: Copy body back to response
            wrappedResponse.copyBodyToResponse();
        }
    }

    /**
     * Logs the details of the request and response.
     * This includes the start and end times, duration, request and response bodies and headers,
     * and other relevant information into an {@link AuditLog} entity and saves it to the repository.
     *
     * @param request  The request wrapper containing the request details.
     * @param response The response wrapper containing the response details.
     * @param start    The time when the request processing started.
     * @param end      The time when the request processing ended.
     * @param duration The duration of the request processing in milliseconds.
     */
    private void logDetails(ContentCachingRequestWrapper request,
                            ContentCachingResponseWrapper response,
                            Instant start, Instant end, long duration) {

        String startTime = FORMATTER.format(start);
        String endTime = FORMATTER.format(end);

        // Request body
        String requestBody = new String(request.getContentAsByteArray(), StandardCharsets.UTF_8);

        // Request headers
        StringBuilder reqHeaders = new StringBuilder();
        request.getHeaderNames().asIterator().forEachRemaining(name ->
                reqHeaders.append(name).append(": ").append(request.getHeader(name)).append("\n"));

        // Response body
        String contentType = response.getContentType();
        String responseBody = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);

        if (contentType != null && contentType.contains("application/json")) {
            try {
                ObjectMapper mapper = new ObjectMapper();

                // Parse the JSON string into a JsonNode
                JsonNode jsonNode = mapper.readTree(responseBody);

                // Save the JsonNode as JSON string (properly formatted)
                responseBody = mapper.writeValueAsString(jsonNode);

            } catch (JsonProcessingException e) {
                // If parsing fails, save a placeholder
                responseBody = "[Invalid JSON response]";
            }
        } else {
            responseBody = "[Non-JSON response not logged]";
        }


        // Response headers
        StringBuilder resHeaders = new StringBuilder();
        response.getHeaderNames().forEach(name ->
                resHeaders.append(name).append(": ").append(response.getHeader(name)).append("\n"));
        String userEmail = "anonymous"; // fallback
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            userEmail = auth.getName(); // Usually the `sub` claim (email/username)
        }
        AuditLog log = AuditLog.builder()
                .userName(userEmail)
                .method(request.getMethod())
                .apiPath(request.getRequestURI())
                .queryString(request.getQueryString())
                .responseStatus(response.getStatus())
                .startTime(start.atZone(ZoneId.systemDefault()).toLocalDateTime())
                .endTime(end.atZone(ZoneId.systemDefault()).toLocalDateTime())
                .durationMs(duration)
                .requestHeaders(reqHeaders.toString())
                .requestBody(requestBody)
                .responseHeaders(resHeaders.toString())
                .responseBody(responseBody)
                .build();
        repo.save(log);
    }
}
