package com.middleware.backend.kaotocamel.service;

import com.middleware.backend.integrated_systems.dto.IntegratedSystemDto;
import com.middleware.backend.integrated_systems.service.IntegratedSystemService;
import com.middleware.backend.kaotocamel.dto.AnalyticsResponseDto;
import com.middleware.backend.kaotocamel.dto.ItemDto;
import com.middleware.backend.kaotocamel.model.IntegratedApi;
import com.middleware.backend.kaotocamel.repository.IntegratedApiRepository;
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
 * Service for DHIS2 API interactions with dynamic URL building
 * Each API uses its own integratedSystem for connection
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Dhis2ClientService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final IntegratedApiRepository integratedApiRepository;
    private final IntegratedSystemService service;

    /**
     * Execute API call with auto-generated URL from frontend parameters
     * This method STILL uses explicit dhis2Code (for direct analytics endpoint)
     */
    public Map<String, Object> executeAnalyticsCall(
            String dhis2Code,
            String dx,
            String ou,
            String pe,
            Map<String, String> attributes,
            Map<String, String> otherParams) {

        try {
            IntegratedSystemDto dto = service.getById(dhis2Code);
            if (dto == null) {
                throw new RuntimeException("No DHIS2 config found for code: " + dhis2Code);
            }

            String baseUrl = dto.getProtocol() + "://" + dto.getHost();
            String analyticsUrl = buildAnalyticsUrlFromParams(dx, ou, pe, attributes, otherParams);
            String fullUrl = baseUrl + analyticsUrl;

            log.info("Generated Analytics URL: {}", fullUrl);

            HttpHeaders headers = createAuthHeaders(dto);
            disableSSLVerification();

            ResponseEntity<String> response = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            AnalyticsResponseDto analytics = objectMapper.readValue(
                    response.getBody(),
                    AnalyticsResponseDto.class
            );

            Map<String, Object> result = new HashMap<>();
            result.put("type", "ANALYTICS");
            result.put("data", analytics);

            return result;

        } catch (Exception e) {
            log.error("Error executing analytics call: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to execute analytics call: " + e.getMessage());
        }
    }

    /**
     * Build Analytics URL with strict ordering
     */
    private String buildAnalyticsUrlFromParams(
            String dx,
            String ou,
            String pe,
            Map<String, String> attributes,
            Map<String, String> otherParams) {

        StringBuilder url = new StringBuilder("/api/analytics.json?");
        List<String> parts = new ArrayList<>();

        if (dx == null || dx.trim().isEmpty()) {
            throw new IllegalArgumentException("dx parameter is required");
        }
        parts.add("dimension=dx:" + dx.trim());

        if (ou != null && !ou.trim().isEmpty()) {
            parts.add("dimension=ou:" + ou.trim());
        }

        if (pe != null && !pe.trim().isEmpty()) {
            parts.add("dimension=pe:" + pe.trim());
        }

        if (attributes != null && !attributes.isEmpty()) {
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                String attrUid = entry.getKey();
                String attrValue = entry.getValue();
                if (attrUid != null && !attrUid.trim().isEmpty()
                        && attrValue != null && !attrValue.trim().isEmpty()) {
                    parts.add("dimension=" + attrUid.trim() + ":" + attrValue.trim());
                }
            }
        }

        if (otherParams != null && !otherParams.isEmpty()) {
            for (Map.Entry<String, String> entry : otherParams.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key != null && !key.trim().isEmpty()
                        && value != null && !value.trim().isEmpty()) {
                    parts.add(key.trim() + "=" + value.trim());
                }
            }
        }

        url.append(String.join("&", parts));
        log.debug("Built Analytics URL: {}", url);
        return url.toString();
    }

    /**
     * Legacy method for backward compatibility
     * Uses systemCode parameter (which is now integratedSystem from API)
     */
    @Deprecated
    public Map<String, Object> executeApiCall(IntegratedApi api, String periodParam, String systemCode) {
        return executeApiCall(api, periodParam, systemCode, null, null, false, false);
    }

    /**
     * Main API call method - uses systemCode (integratedSystem) instead of global dhis2Code
     */
    @Deprecated
    public Map<String, Object> executeApiCall(
            IntegratedApi api,
            String periodParam,
            String systemCode,  // This is now integratedSystem from API
            String ouFromRequest,
            String peFromRequest,
            boolean useOuFromRequest,
            boolean usePeFromRequest) {
        try {
            // Use systemCode (which is api.getIntegratedSystem()) to load config
            IntegratedSystemDto dto = service.getById(systemCode);
            if (dto == null) {
                throw new RuntimeException("No DHIS2 config found for integratedSystem: " + systemCode);
            }

            String baseUrl = dto.getProtocol() + "://" + dto.getHost();
            HttpHeaders headers = createAuthHeaders(dto);

            log.info("Using DHIS2 settings: integratedSystem={}, baseUrl={}", systemCode, baseUrl);

            String decodedUrl = decodeUrlIfNeeded(api.getApiUrl());
            String finalUrl = buildAnalyticsUrl(
                    decodedUrl,
                    periodParam,
                    ouFromRequest,
                    peFromRequest,
                    useOuFromRequest,
                    usePeFromRequest
            );

            String fullUrl = buildFullUrl(systemCode, finalUrl, baseUrl);

            disableSSLVerification();

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
    }

    private String buildAnalyticsUrl(
            String apiUrl,
            String periodParam,
            String ouFromRequest,
            String peFromRequest,
            boolean useOuFromRequest,
            boolean usePeFromRequest) {

        String baseEndpoint;
        Map<String, String> dimensions = new LinkedHashMap<>();
        Map<String, String> otherParams = new LinkedHashMap<>();

        if (apiUrl.contains("?")) {
            String[] parts = apiUrl.split("\\?", 2);
            baseEndpoint = parts[0];
            String queryString = parts[1];

            for (String param : queryString.split("&")) {
                if (param.contains("=")) {
                    String[] kv = param.split("=", 2);
                    String key = kv[0].trim();
                    String value = kv.length > 1 ? kv[1].trim() : "";

                    if (key.equals("dimension")) {
                        String[] dimParts = value.split(":", 2);
                        if (dimParts.length == 2) {
                            dimensions.put(dimParts[0], dimParts[1]);
                        }
                    } else {
                        otherParams.put(key, value);
                    }
                }
            }
        } else {
            baseEndpoint = apiUrl;
        }

        List<String> orderedDimensions = new ArrayList<>();

        // 1. dx (always from database)
        String dxValue = dimensions.get("dx");
        if (dxValue != null && !dxValue.isEmpty()) {
            orderedDimensions.add("dimension=dx:" + dxValue);
        }

        // 2. ou - decide based on flag
        String ouValue = null;
        if (useOuFromRequest) {
            if (ouFromRequest != null && !ouFromRequest.trim().isEmpty()) {
                ouValue = ouFromRequest.trim();
            }
        } else {
            if (dimensions.containsKey("ou")) {
                ouValue = dimensions.get("ou");
            }
        }
        if (ouValue != null && !ouValue.isEmpty()) {
            orderedDimensions.add("dimension=ou:" + ouValue);
        }

        // 3. pe - decide based on flag
        String peValue = null;
        if (usePeFromRequest) {
            if (peFromRequest != null && !peFromRequest.trim().isEmpty()) {
                peValue = peFromRequest.trim().replaceAll("[^a-zA-Z0-9_\\-.,;]", "").replace(",", ";");
            }
        } else {
            if (dimensions.containsKey("pe")) {
                peValue = dimensions.get("pe");
            }
        }
        if (peValue != null && !peValue.isEmpty()) {
            orderedDimensions.add("dimension=pe:" + peValue);
        }

        // 4. Other dimensions (attributes)
        for (Map.Entry<String, String> entry : dimensions.entrySet()) {
            String key = entry.getKey();
            if (!key.equals("dx") && !key.equals("ou") && !key.equals("pe")) {
                orderedDimensions.add("dimension=" + key + ":" + entry.getValue());
            }
        }

        // 5. Other params
        List<String> otherParamsList = new ArrayList<>();
        for (Map.Entry<String, String> entry : otherParams.entrySet()) {
            otherParamsList.add(entry.getKey() + "=" + entry.getValue());
        }

        StringBuilder finalUrl = new StringBuilder(baseEndpoint);
        if (!orderedDimensions.isEmpty() || !otherParamsList.isEmpty()) {
            finalUrl.append("?");
            finalUrl.append(String.join("&", orderedDimensions));
            if (!otherParamsList.isEmpty()) {
                if (!orderedDimensions.isEmpty()) {
                    finalUrl.append("&");
                }
                finalUrl.append(String.join("&", otherParamsList));
            }
        }

        log.debug("Built Analytics URL with flags (useOu={}, usePe={}): {}",
                useOuFromRequest, usePeFromRequest, finalUrl);

        return finalUrl.toString();
    }

    private String decodeUrlIfNeeded(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        try {
            if (url.contains("%")) {
                return URLDecoder.decode(url, StandardCharsets.UTF_8.name());
            }
            return url;
        } catch (Exception e) {
            log.warn("Failed to decode URL: {}", e.getMessage());
            return url;
        }
    }

    // ============ METADATA & ORG UNITS ============

    /**
     * Fetch org units metadata using integratedSystem from metadata API
     * systemCode is now the integratedSystem from the calling API
     */
    public Map<String, Map<String, Object>> fetchOrganisationUnitsMetadata(
            String systemCode,  // This is integratedSystem from API
            String businessApiCode) {

        Map<String, Map<String, Object>> indexed = new HashMap<>();
        try {
            if (businessApiCode == null || businessApiCode.isEmpty()) {
                return indexed;
            }

            // Find metadata API bound to this business API
            Optional<IntegratedApi> metadataApiOpt =
                    integratedApiRepository.findActiveMetadataByBoundCode(businessApiCode);

            if (metadataApiOpt.isEmpty()) {
                log.debug("No metadata API found for businessApiCode: {}", businessApiCode);
                return indexed;
            }

            IntegratedApi metadataApi = metadataApiOpt.get();

            // IMPORTANT: Use metadata API's own integratedSystem (NOT the passed systemCode)
            String metadataSystemCode = metadataApi.getIntegratedSystem();

            log.info("Fetching metadata using integratedSystem: {} (from metadata API)",
                    metadataSystemCode);

            IntegratedSystemDto dto = service.getById(metadataSystemCode);
            if (dto == null) {
                throw new RuntimeException("No DHIS2 config found for integratedSystem: " + metadataSystemCode);
            }

            String baseUrl = dto.getProtocol() + "://" + dto.getHost();
            HttpHeaders headers = createAuthHeaders(dto);

            String decodedApiUrl = decodeUrlIfNeeded(metadataApi.getApiUrl());
            String metadataUrl = buildFullUrl(metadataSystemCode, decodedApiUrl, baseUrl);

            disableSSLVerification();

            ResponseEntity<String> response = restTemplate.exchange(
                    metadataUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            Map<String, Object> metadata = objectMapper.readValue(response.getBody(), Map.class);
            List<Map<String, Object>> orgUnits = (List<Map<String, Object>>) metadata.get("organisationUnits");

            if (orgUnits != null) {
                for (Map<String, Object> ou : orgUnits) {
                    String id = (String) ou.get("id");
                    if (id != null) {
                        indexed.put(id, ou);
                    }
                }
            }

            log.info("Fetched {} org units from metadata API (integratedSystem: {})",
                    indexed.size(), metadataSystemCode);

            return indexed;

        } catch (Exception e) {
            log.error("Error fetching org units metadata: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    public Map<String, Map<String, Object>> fetchOrganisationUnitsMetadata(String systemCode) {
        return fetchOrganisationUnitsMetadata(systemCode, null);
    }

    // ============ HELPER METHODS ============

    private String buildFullUrl(String integratedSystem, String relativeUrl, String baseUrl) {
        if (!baseUrl.endsWith("/") && !relativeUrl.startsWith("/")) {
            baseUrl += "/";
        } else if (baseUrl.endsWith("/") && relativeUrl.startsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + relativeUrl;
    }

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

    private HttpHeaders createAuthHeaders(IntegratedSystemDto dto) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("User-Agent", "Middleware-DHIS2/1.0");
        headers.set("Cache-Control", "no-cache");

        String authType = String.valueOf(dto.getAuthenticationType());

        if ("JWT".equalsIgnoreCase(authType)) {
            String token = dto.getToken();
            if (token == null) {
                throw new RuntimeException("JWT token is missing");
            }
            headers.set("Authorization", "Bearer " + token);
        } else {
            String username = dto.getUsername();
            String password = dto.getPassword();
            String auth = username + ":" + password;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + new String(encodedAuth));
        }

        return headers;
    }

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

    /**
     * Fetch categoryOption codes from DHIS2 for given IDs
     * Calls: /api/categoryOptions.json?filter=id:in:[ids]&fields=id,name,code&paging=false
     * 
     * @param systemCode The integrated system code (e.g., DHIS2.DWH)
     * @param categoryOptionIds Set of categoryOption IDs to fetch codes for
     * @return Map of categoryOption ID to code
     */
    public Map<String, String> fetchCategoryOptionCodes(String systemCode, Set<String> categoryOptionIds) {
        Map<String, String> idToCodeMap = new HashMap<>();
        
        if (categoryOptionIds == null || categoryOptionIds.isEmpty()) {
            return idToCodeMap;
        }
        
        try {
            IntegratedSystemDto dto = service.getById(systemCode);
            if (dto == null) {
                log.warn("No DHIS2 config found for systemCode: {}", systemCode);
                return idToCodeMap;
            }
            
            String baseUrl = dto.getProtocol() + "://" + dto.getHost();
            HttpHeaders headers = createAuthHeaders(dto);
            
            // Build filter: id:in:[id1,id2,id3]
            String idsJoined = String.join(",", categoryOptionIds);
            String apiUrl = "/api/categoryOptions.json?filter=id:in:[" + idsJoined + "]&fields=id,name,code&paging=false";
            
            String fullUrl = baseUrl + apiUrl;
            
            log.debug("Fetching categoryOption codes from: {}", fullUrl);
            
            disableSSLVerification();
            
            ResponseEntity<String> response = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            
            // Parse response
            if (response.getBody() != null) {
                Map<String, Object> parsed = objectMapper.readValue(response.getBody(), Map.class);
                List<Map<String, Object>> categoryOptions = (List<Map<String, Object>>) parsed.get("categoryOptions");
                
                if (categoryOptions != null) {
                    for (Map<String, Object> option : categoryOptions) {
                        String id = (String) option.get("id");
                        String code = (String) option.get("code");
                        if (id != null && code != null) {
                            idToCodeMap.put(id, code);
                        }
                    }
                }
            }
            
            log.info("Fetched {} categoryOption codes from DHIS2 (systemCode: {})", 
                    idToCodeMap.size(), systemCode);
            
        } catch (Exception e) {
            log.error("Error fetching categoryOption codes: {}", e.getMessage());
        }
        
        return idToCodeMap;
    }

    /**
     * Extract attribute dimension IDs from API URL
     * Example: dimension=UedUhlkhYWX:KG7l26av4v5;M0TzDNQdeju;NDWlje7qHvH
     * Returns: Set of [KG7l26av4v5, M0TzDNQdeju, NDWlje7qHvH]
     */
    public Set<String> extractAttributeIdsFromApiUrl(String apiUrl) {
        Set<String> attributeIds = new HashSet<>();
        
        if (apiUrl == null || apiUrl.isEmpty()) {
            return attributeIds;
        }
        
        try {
            // Decode URL if needed
            String decodedUrl = decodeUrlIfNeeded(apiUrl);
            
            // Find all dimension parameters
            String[] parts = decodedUrl.split("[?&]");
            for (String part : parts) {
                if (part.startsWith("dimension=")) {
                    String dimValue = part.substring("dimension=".length());
                    String[] dimParts = dimValue.split(":", 2);
                    
                    if (dimParts.length == 2) {
                        String dimKey = dimParts[0];
                        String dimValues = dimParts[1];
                        
                        // Skip dx, ou, pe - these are not attribute dimensions
                        if (!dimKey.equals("dx") && !dimKey.equals("ou") && !dimKey.equals("pe")) {
                            // This is an attribute dimension - extract the IDs
                            String[] ids = dimValues.split(";");
                            for (String id : ids) {
                                if (id != null && !id.trim().isEmpty()) {
                                    attributeIds.add(id.trim());
                                }
                            }
                        }
                    }
                }
            }
            
            log.debug("Extracted {} attribute IDs from API URL", attributeIds.size());
            
        } catch (Exception e) {
            log.warn("Error extracting attribute IDs from URL: {}", e.getMessage());
        }
        
        return attributeIds;
    }
}