package com.middleware.backend.kaotocamel.service;

import com.middleware.backend.kaotocamel.dto.AnalyticsResponseDto;
import com.middleware.backend.kaotocamel.dto.ItemDto;
import com.middleware.backend.kaotocamel.model.IntegratedApi;
import com.middleware.backend.kaotocamel.repository.IntegratedApiRepository;
import com.middleware.backend.system_settings.service.ConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import javax.net.ssl.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Service for DHIS2 API interactions with dynamic configuration support
 * Enhanced with boundApiCode feature for metadata API binding
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Dhis2ClientService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ConfigService configService;
    private final IntegratedApiRepository integratedApiRepository;

    /**
     * Execute API call with period injection
     */
    public Map<String, Object> executeApiCall(IntegratedApi api, String periodParam, String dhis2Code) {
        try {
            Map<String, String> dhis2Map = configService.getModuleConfig("dhis2", dhis2Code);
            if (dhis2Map.isEmpty()) {
                throw new RuntimeException("No DHIS2 config found for code: " + dhis2Code);
            }

            String baseUrl = dhis2Map.get("baseUrl");
            String username = dhis2Map.get("userName");
            String password = dhis2Map.get("password");

            log.info("Using DHIS2 settings: code={}, baseUrl={}, userName={}", dhis2Code, baseUrl, username);

             String decodedUrl = decodeUrlIfNeeded(api.getApiUrl());

            // Inject period into URL if needed
            String finalUrl = injectPeriodInUrl(decodedUrl, periodParam);

            // Build full URL
            String fullUrl = buildFullUrl(api.getIntegratedSystem(), finalUrl, baseUrl);

            disableSSLVerification();

            HttpHeaders headers = createHeaders(username, password);

            ResponseEntity<String> response = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            return parseResponse(response.getBody(), api.getType());

        } catch (HttpClientErrorException e) {
            log.error("DHIS2 API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("DHIS2 API error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error executing API call for {}: {}", api.getCode(), e.getMessage());
            throw new RuntimeException("Failed to execute API call: " + e.getMessage());
        }
    }private String decodeUrlIfNeeded(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        try {
            String processedUrl = url;

            if (processedUrl.contains("%")) {
                log.debug("Detected encoded URL, decoding...");
                processedUrl = URLDecoder.decode(processedUrl, StandardCharsets.UTF_8.name());
                log.debug("Decoded URL: {}", processedUrl);
            }

            processedUrl = replacePeriodWithPlaceholder(processedUrl);

            return processedUrl;

        } catch (Exception e) {
            log.warn("Failed to process URL, using original: {}", e.getMessage());
            return url;
        }
    }
    private String replacePeriodWithPlaceholder(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }


        if (url.contains("{{PERIOD}}")) {
            log.debug("URL already contains {{PERIOD}} placeholder");
            return url;
        }


         String periodPattern = "(pe[=:])([^&;\\s]+)";

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(periodPattern);
        java.util.regex.Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            String originalPeriod = matcher.group(2);
            log.debug("Found period '{}' in URL, replacing with {{PERIOD}}", originalPeriod);

             String result = matcher.replaceFirst("$1{{PERIOD}}");
            log.debug("Replaced URL: {}", result);
            return result;
        }

        log.debug("No period found in URL to replace");
        return url;
    }
    /**
     * Fetch organisation units metadata from DHIS2
     * Uses dynamic metadata URL from IntegratedApi configuration if available
     *
     * @param dhis2Code The DHIS2 configuration code
     * @param businessApiCode Optional business API code to fetch specific metadata configuration
     * @return Map of organisation units indexed by ID
     */
    public Map<String, Map<String, Object>> fetchOrganisationUnitsMetadata(String dhis2Code, String businessApiCode) {
        try {
            if (businessApiCode != null && !businessApiCode.isEmpty()) {
                Optional<IntegratedApi> metadataApi = integratedApiRepository
                        .findActiveMetadataByBoundCode(businessApiCode);

                if (metadataApi.isPresent()) {
                    log.info("Using configured metadata API for business code: {}", businessApiCode);
                    IntegratedApi api = metadataApi.get();

                    Map<String, String> dhis2Map = configService.getModuleConfig("dhis2", dhis2Code);
                    if (dhis2Map.isEmpty()) {
                        throw new RuntimeException("No DHIS2 config found for code: " + dhis2Code);
                    }

                    String baseUrl = dhis2Map.get("baseUrl");
                    String username = dhis2Map.get("userName");
                    String password = dhis2Map.get("password");

                     String decodedApiUrl = decodeUrlIfNeeded(api.getApiUrl());

                    // Check if period injection is needed
                    String apiUrl = decodedApiUrl;
                    if (apiUrl.contains("{{PERIOD}}")) {
                        apiUrl = apiUrl.replace("{{PERIOD}}", "");
                    }

                    // Use the configured metadata URL
                    String metadataUrl = buildFullUrl(api.getIntegratedSystem(), apiUrl, baseUrl);

                    log.info("Fetching organisation units metadata from: {}", metadataUrl);

                    disableSSLVerification();
                    HttpHeaders headers = createHeaders(username, password);

                    ResponseEntity<String> response = restTemplate.exchange(
                            metadataUrl,
                            HttpMethod.GET,
                            new HttpEntity<>(headers),
                            String.class
                    );

                    // Parse response
                    Map<String, Object> metadata = objectMapper.readValue(response.getBody(), Map.class);
                    List<Map<String, Object>> orgUnits = (List<Map<String, Object>>) metadata.get("organisationUnits");

                    // Index by ID for fast lookup
                    Map<String, Map<String, Object>> indexed = new HashMap<>();
                    if (orgUnits != null) {
                        for (Map<String, Object> ou : orgUnits) {
                            String id = (String) ou.get("id");
                            if (id != null) {
                                indexed.put(id, ou);
                            }
                        }
                    }

                    log.info("Loaded {} organisation units metadata", indexed.size());
                    return indexed;

                } else {
                    log.warn("No metadata API configured for business code: {}. Returning empty map.", businessApiCode);
                    return new HashMap<>();
                }
            } else {
                log.warn("No business API code provided for metadata fetch. Returning empty map.");
                return new HashMap<>();
            }

        } catch (Exception e) {
            log.error("Error fetching organisation units metadata: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * Overloaded method for backward compatibility
     */
    public Map<String, Map<String, Object>> fetchOrganisationUnitsMetadata(String dhis2Code) {
        return fetchOrganisationUnitsMetadata(dhis2Code, null);
    }

    /**
     * Inject period parameter into URL
     */
    private String injectPeriodInUrl(String apiUrl, String periodParam) {
        // If URL doesn't contain period placeholder, return as is
        if (!apiUrl.contains("{{PERIOD}}")) {
            return apiUrl;
        }

        if (periodParam == null || periodParam.isEmpty()) {
            throw new IllegalArgumentException("Period parameter is required for this API");
        }

        String cleaned = periodParam.replaceAll("[^a-zA-Z0-9_\\-.,;]", "");
        String formatted = cleaned.replace(",", ";");
        return apiUrl.replace("{{PERIOD}}", formatted);
    }

    /**
     * Build full URL from components
     */
    private String buildFullUrl(String integratedSystem, String relativeUrl, String baseUrl) {
        // Handle various URL formats
        if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
            // Already a full URL
            return relativeUrl;
        }

        // Ensure proper URL construction
        if (!baseUrl.endsWith("/") && !relativeUrl.startsWith("/")) {
            baseUrl += "/";
        } else if (baseUrl.endsWith("/") && relativeUrl.startsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        // Prepend https:// if not present
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            return "https://" + baseUrl + relativeUrl;
        }

        return baseUrl + relativeUrl;
    }

    /**
     * Create HTTP headers with authentication
     */
    private HttpHeaders createHeaders(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + new String(encodedAuth));

        headers.set("User-Agent", "Middleware-DHIS2/1.0");
        headers.set("Cache-Control", "no-cache");
        return headers;
    }

    /**
     * Parse API response based on type
     */
    private Map<String, Object> parseResponse(String responseBody, IntegratedApi.ApiType apiType) throws Exception {
        Map<String, Object> response = new HashMap<>();

        switch (apiType) {
            case ANALYTICS:
                AnalyticsResponseDto analytics = objectMapper.readValue(responseBody, AnalyticsResponseDto.class);
                response.put("type", "ANALYTICS");
                response.put("data", analytics);
                break;
            case METADATA:
                Map<String, Object> metadata = objectMapper.readValue(responseBody, Map.class);
                response.put("type", "METADATA");
                response.put("data", metadata);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported API type: " + apiType);
        }

        return response;
    }

    /**
     * Disable SSL verification for development environments
     * WARNING: Should not be used in production
     */
    private void disableSSLVerification() {
        try {
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
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            log.warn("Failed to disable SSL verification: {}", e.getMessage());
        }
    }

    /**
     * Get organisation unit name from analytics metadata
     */
    public String getOrgUnitName(AnalyticsResponseDto analytics, String ouId) {
        try {
            if (analytics.getMetaData() != null && analytics.getMetaData().getItems() != null) {
                ItemDto item = analytics.getMetaData().getItems().get(ouId);
                return item != null ? item.getName() : null;
            }
        } catch (Exception e) {
            log.error("Error getting org unit name: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Get period name from analytics metadata
     */
    public String getPeriodName(AnalyticsResponseDto analytics, String peId) {
        try {
            if (analytics.getMetaData() != null && analytics.getMetaData().getItems() != null) {
                ItemDto item = analytics.getMetaData().getItems().get(peId);
                return item != null ? item.getName() : peId;
            }
        } catch (Exception e) {
            log.error("Error getting period name: {}", e.getMessage());
        }
        return peId;
    }
}