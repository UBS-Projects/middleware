//package com.middleware.backend.kaotocamel.service;
//
//import com.middleware.backend.kaotocamel.dto.AnalyticsResponseDto;
//import com.middleware.backend.kaotocamel.dto.ItemDto;
//import com.middleware.backend.kaotocamel.model.IntegratedApi;
//import com.middleware.backend.system_settings.model.Config;
//import com.middleware.backend.system_settings.service.ConfigService;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.*;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//import org.springframework.web.client.HttpClientErrorException;
//
//import javax.net.ssl.*;
//import java.nio.charset.StandardCharsets;
//import java.security.SecureRandom;
//import java.security.cert.X509Certificate;
//import java.util.*;
//
///**
// * Refactored service for DHIS2 APIs using ConfigService directly
// */
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class Dhis2ClientService {
//
//    private final RestTemplate restTemplate;
//    private final ObjectMapper objectMapper;
//    private final ConfigService configService;
//
//    /**
//     * Execute API call with period injection
//     */
//    public Map<String, Object> executeApiCall(IntegratedApi api, String periodParam, String dhis2Code) {
//        try {
//             Map<String, String> dhis2Map = configService.getModuleConfig("dhis2", dhis2Code);
//            if (dhis2Map.isEmpty()) {
//                throw new RuntimeException("No DHIS2 config found for code: " + dhis2Code);
//            }
//
//            String baseUrl = dhis2Map.get("baseUrl");
//            String username = dhis2Map.get("userName");
//            String password = dhis2Map.get("password");
//
//            log.info("Using DHIS2 settings: code={}, baseUrl={}, userName={}", dhis2Code, baseUrl, username);
//
//            // Inject period into URL
//            String finalUrl = injectPeriodInUrl(api.getApiUrl(), periodParam);
//
//            // Build full URL
//            String fullUrl = buildFullUrl(api.getIntegratedSystem(), finalUrl, baseUrl);
//
//            disableSSLVerification();
//
//            HttpHeaders headers = createHeaders(username, password);
//
//            ResponseEntity<String> response = restTemplate.exchange(
//                    fullUrl,
//                    HttpMethod.GET,
//                    new HttpEntity<>(headers),
//                    String.class
//            );
//
//            return parseResponse(response.getBody(), api.getType());
//
//        } catch (HttpClientErrorException e) {
//            log.error("DHIS2 API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
//            throw new RuntimeException("DHIS2 API error: " + e.getMessage());
//        } catch (Exception e) {
//            log.error("Error executing API call for {}: {}", api.getCode(), e.getMessage());
//            throw new RuntimeException("Failed to execute API call: " + e.getMessage());
//        }
//    }
//
//
//    private String injectPeriodInUrl(String apiUrl, String periodParam) {
//        if (periodParam == null || periodParam.isEmpty()) {
//            throw new IllegalArgumentException("Period parameter is required");
//        }
//
//        String cleaned = periodParam.replaceAll("[^a-zA-Z0-9_\\-.,;]", "");
//        String formatted = cleaned.replace(",", ";");
//        return apiUrl.replace("{{PERIOD}}", formatted);
//    }
//
//    private String buildFullUrl(String integratedSystem, String relativeUrl, String baseUrl) {
//        if (!baseUrl.endsWith("/") && !relativeUrl.startsWith("/")) {
//            baseUrl += "/";
//        }
//        return "https://" + baseUrl + relativeUrl;
//    }
//
//    private HttpHeaders createHeaders(String username, String password) {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
//
//        String auth = username + ":" + password;
//        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
//        headers.set("Authorization", "Basic " + new String(encodedAuth));
//
//        headers.set("User-Agent", "Middleware-DHIS2/1.0");
//        headers.set("Cache-Control", "no-cache");
//        return headers;
//    }
//
//    private Map<String, Object> parseResponse(String responseBody, IntegratedApi.ApiType apiType) throws Exception {
//        Map<String, Object> response = new HashMap<>();
//
//        switch (apiType) {
//            case ANALYTICS:
//                AnalyticsResponseDto analytics = objectMapper.readValue(responseBody, AnalyticsResponseDto.class);
//                response.put("type", "ANALYTICS");
//                response.put("data", analytics);
//                break;
//            case DATAVALUE:
//                Map<String, Object> dataValues = objectMapper.readValue(responseBody, Map.class);
//                response.put("type", "DATAVALUE");
//                response.put("data", dataValues);
//                break;
//            case METADATA:
//                Map<String, Object> metadata = objectMapper.readValue(responseBody, Map.class);
//                response.put("type", "METADATA");
//                response.put("data", metadata);
//                break;
//            default:
//                throw new UnsupportedOperationException("Unsupported API type: " + apiType);
//        }
//
//        return response;
//    }
//
//    private void disableSSLVerification() {
//        try {
//            TrustManager[] trustAllCerts = new TrustManager[]{
//                    new X509TrustManager() {
//                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
//                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
//                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
//                    }
//            };
//            SSLContext sc = SSLContext.getInstance("TLS");
//            sc.init(null, trustAllCerts, new SecureRandom());
//            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
//            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
//        } catch (Exception e) {
//            log.warn("Failed to disable SSL verification: {}", e.getMessage());
//        }
//    }
//
//    public String getOrgUnitName(AnalyticsResponseDto analytics, String ouId) {
//        try {
//            if (analytics.getMetaData() != null && analytics.getMetaData().getItems() != null) {
//                ItemDto item = analytics.getMetaData().getItems().get(ouId);
//                return item != null ? item.getName() : null;
//            }
//        } catch (Exception e) { log.error("Error getting org unit name: {}", e.getMessage()); }
//        return null;
//    }
//
//    public String getPeriodName(AnalyticsResponseDto analytics, String peId) {
//        try {
//            if (analytics.getMetaData() != null && analytics.getMetaData().getItems() != null) {
//                ItemDto item = analytics.getMetaData().getItems().get(peId);
//                return item != null ? item.getName() : peId;
//            }
//        } catch (Exception e) { log.error("Error getting period name: {}", e.getMessage()); }
//        return peId;
//    }
//}
