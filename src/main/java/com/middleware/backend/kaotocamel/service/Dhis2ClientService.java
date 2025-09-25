package com.middleware.backend.kaotocamel.service;

import com.middleware.backend.kaotocamel.dto.*;
import com.middleware.backend.kaotocamel.model.IntegratedApi;
import com.middleware.backend.system_settings.config.Dhis2Config;
import com.middleware.backend.system_settings.model.Dhis2;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Service for communicating with DHIS2 APIs
 * Handles period injection, API calls, and response parsing
 * Updated to use dynamic DHIS2 configuration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Dhis2ClientService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Dhis2Config dhis2Config;

    /**
     * Execute API call with period injection using dynamic DHIS2 settings
     */
    public Map<String, Object> executeApiCall(IntegratedApi api, String periodParam) {
        try {
             dhis2Config.refreshSettings();
            Dhis2 currentSettings = dhis2Config.getCurrentSettings();

            log.info("Using DHIS2 settings: baseUrl={}, userName={}",
                    currentSettings.getBaseUrl(), currentSettings.getUserName());

            // Inject period into URL
            String finalUrl = injectPeriodInUrl(api.getApiUrl(), periodParam);

            // Build complete URL using dynamic settings
            String fullUrl = buildFullUrl(api.getIntegratedSystem(), finalUrl, currentSettings);

            log.info("Executing DHIS2 API call: {}", fullUrl);

            // ✅ تعطيل SSL verification للتطوير
            disableSSLVerification();

            // Prepare headers with dynamic authentication
            HttpHeaders headers = createHeaders(currentSettings);

            // Execute request
            ResponseEntity<String> response = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            // Parse response based on API type
            return parseResponse(response.getBody(), api.getType());

        } catch (HttpClientErrorException e) {
            log.error("DHIS2 API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("DHIS2 API error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error executing API call for {}: {}", api.getCode(), e.getMessage());
            throw new RuntimeException("Failed to execute API call: " + e.getMessage());
        }
    }

    /**
     * Inject period parameter into API URL
     */
    private String injectPeriodInUrl(String apiUrl, String periodParam) {
        if (periodParam == null || periodParam.isEmpty()) {
            throw new IllegalArgumentException("Period parameter is required");
        }

        log.debug("Original URL: {}", apiUrl);
        log.debug("Period parameter: {}", periodParam);

        // Clean and validate period parameter
        String cleanedPeriod = sanitizePeriodParam(periodParam);
        String formattedPeriod = formatPeriodForDhis2(cleanedPeriod);

        String finalUrl = apiUrl.replace("{{PERIOD}}", formattedPeriod);

        if (finalUrl.equals(apiUrl)) {
            log.debug("No {{PERIOD}} placeholder found, URL unchanged");
        } else {
            log.debug("Replaced {{PERIOD}} with: {}", formattedPeriod);
        }

        log.debug("Final URL: {}", finalUrl);
        return finalUrl;
    }

    /**
     * Sanitize period parameter - allow only safe characters
     */
    private String sanitizePeriodParam(String period) {
        if (period == null) {
            return null;
        }

        // Allow alphanumeric, underscore, hyphen, dot, comma, semicolon
        String cleaned = period.replaceAll("[^a-zA-Z0-9_\\-.,;]", "");

        if (!cleaned.equals(period)) {
            log.warn("Period parameter was sanitized: {} -> {}", period, cleaned);
        }

        return cleaned;
    }

    /**
     * Format period for DHIS2 (convert commas to semicolons for lists)
     */
    private String formatPeriodForDhis2(String period) {
        if (period == null || period.isEmpty()) {
            return period;
        }

        // Convert comma-separated list to semicolon-separated (DHIS2 format)
        if (period.contains(",")) {
            String formatted = period.replace(",", ";");
            log.debug("Formatted period list: {} -> {}", period, formatted);
            return formatted;
        }

        return period;
    }

    /**
     * Build full URL based on integrated system using dynamic settings
     */
    private String buildFullUrl(String integratedSystem, String relativeUrl, Dhis2 currentSettings) {
        String baseUrl;

         if ("HMIS-DWH".equals(integratedSystem)) {
             baseUrl = "https://" + currentSettings.getBaseUrl();
            log.debug("Using HMIS-DWH system: {}", baseUrl);
        } else if ("DHIS2".equals(integratedSystem)) {
             baseUrl = "https://" + currentSettings.getBaseUrl();
            log.debug("Using DHIS2 system: {}", baseUrl);
        } else {
             baseUrl = "https://" + currentSettings.getBaseUrl();
            log.debug("Using default system: {}", baseUrl);
        }

        // Ensure proper URL construction
        if (!baseUrl.endsWith("/") && !relativeUrl.startsWith("/")) {
            baseUrl += "/";
        }

        return baseUrl + relativeUrl;
    }

    /**
     * Create HTTP headers with dynamic authentication
     */
    private HttpHeaders createHeaders(Dhis2 currentSettings) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

        // ✅ Add dynamic basic authentication
        String auth = currentSettings.getUserName() + ":" + currentSettings.getPassword();
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + new String(encodedAuth);
        headers.set("Authorization", authHeader);

        // Add additional headers for better compatibility
        headers.set("User-Agent", "Middleware-DHIS2/1.0");
        headers.set("Cache-Control", "no-cache");

        log.debug("Created headers with auth for user: {}", currentSettings.getUserName());
        return headers;
    }

    /**
     * Parse response based on API type
     */
    private Map<String, Object> parseResponse(String responseBody, IntegratedApi.ApiType apiType) {
        try {
            Map<String, Object> response = new HashMap<>();

            switch (apiType) {
                case ANALYTICS:
                    AnalyticsResponseDto analytics = objectMapper.readValue(
                            responseBody, AnalyticsResponseDto.class);
                    response.put("type", "ANALYTICS");
                    response.put("data", analytics);
                    break;

                case DATAVALUE:
                    // Parse data value response
                    Map<String, Object> dataValues = objectMapper.readValue(
                            responseBody, Map.class);
                    response.put("type", "DATAVALUE");
                    response.put("data", dataValues);
                    break;

                case METADATA:
                    // Parse metadata response
                    Map<String, Object> metadata = objectMapper.readValue(
                            responseBody, Map.class);
                    response.put("type", "METADATA");
                    response.put("data", metadata);
                    break;

                default:
                    throw new UnsupportedOperationException(
                            "Unsupported API type: " + apiType);
            }

            return response;

        } catch (Exception e) {
            log.error("Error parsing DHIS2 response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse DHIS2 response: " + e.getMessage());
        }
    }

    /**
     * Get organization unit name from metadata
     */
    public String getOrgUnitName(AnalyticsResponseDto analytics, String ouId) {
        try {
            if (analytics.getMetaData() != null &&
                    analytics.getMetaData().getItems() != null) {

                ItemDto item = analytics.getMetaData().getItems().get(ouId);
                if (item != null) {
                    return item.getName();
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Error getting org unit name: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get period name from metadata
     */
    public String getPeriodName(AnalyticsResponseDto analytics, String peId) {
        try {
            if (analytics.getMetaData() != null &&
                    analytics.getMetaData().getItems() != null) {

                ItemDto item = analytics.getMetaData().getItems().get(peId);
                if (item != null) {
                    return item.getName();
                }
            }
            return peId; // Return ID if name not found
        } catch (Exception e) {
            log.error("Error getting period name: {}", e.getMessage());
            return peId;
        }
    }

    /**
     * ✅ Disable SSL verification for development (same as Dhis2CsvDryRunThenCommit)
     */
    private void disableSSLVerification() {
        try {
            log.debug("Disabling SSL verification for DHIS2 connection");

            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            };

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> {
                log.debug("SSL: Accepting hostname {}", hostname);
                return true;
            });

        } catch (Exception e) {
            log.warn("Failed to disable SSL verification: {}", e.getMessage());
        }
    }

}