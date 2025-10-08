package com.middleware.backend.kaotocamel.service;

import com.middleware.backend.kaotocamel.dto.*;
import com.middleware.backend.kaotocamel.model.*;
import com.middleware.backend.kaotocamel.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Main service for processing middleware API requests
 * Orchestrates data fetching, mapping, and aggregation
 * Enhanced to use dynamic metadata URL based on business API code
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MiddlewareProcessorService {

    private final IntegrationMappingRepository mappingRepository;
    private final Dhis2ClientService dhis2Client;

    @Transactional(readOnly = true)
    public MiddlewareResponseDto processMiddlewareRequest(
            String middlewareApiName,
            String periodParam,
            String dhis2Code) {

        log.info("Processing middleware request for API: {} with period: {} and DHIS2 code: {}",
                middlewareApiName, periodParam, dhis2Code);

        if (periodParam == null || periodParam.trim().isEmpty()) {
            throw new IllegalArgumentException("Parameter 'pe' is required");
        }

        if (dhis2Code == null || dhis2Code.trim().isEmpty()) {
            throw new IllegalArgumentException("DHIS2 code is required");
        }

        try {
            // Step 1: Load mappings
            List<IntegrationMapping> mappings = mappingRepository
                    .findActiveMiddlewareMappings(middlewareApiName);

            if (mappings.isEmpty()) {
                log.warn("No active mappings found for API: {}", middlewareApiName);
                return MiddlewareResponseDto.builder()
                        .rows(new ArrayList<>())
                        .build();
            }

            // Step 2: Fetch organisation units metadata
            // Pass the business API name to potentially use configured metadata API
            Map<String, Map<String, Object>> orgUnitsMetadata =
                    dhis2Client.fetchOrganisationUnitsMetadata(dhis2Code, middlewareApiName);

            // Step 3: Group mappings by API
            Map<IntegratedApi, List<IntegrationMapping>> mappingsByApi =
                    groupMappingsByApi(mappings);

            // Step 4: Execute DHIS2 calls
            Map<IntegratedApi, Map<String, Object>> apiResponses =
                    executeDhis2Calls(mappingsByApi.keySet(), periodParam, dhis2Code);

            // Step 5: Build response with enriched orgUnit data
            return buildMiddlewareResponse(mappingsByApi, apiResponses, orgUnitsMetadata);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing middleware request: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process middleware request: " + e.getMessage());
        }
    }

    /**
     * Group mappings by integrated API
     */
    private Map<IntegratedApi, List<IntegrationMapping>> groupMappingsByApi(
            List<IntegrationMapping> mappings) {

        Map<IntegratedApi, List<IntegrationMapping>> grouped = new LinkedHashMap<>();

        for (IntegrationMapping mapping : mappings) {
            IntegratedApi api = mapping.getIntegratedApi();
            grouped.computeIfAbsent(api, k -> new ArrayList<>()).add(mapping);
        }

        log.info("Grouped {} mappings into {} API calls", mappings.size(), grouped.size());
        return grouped;
    }

    /**
     * Execute DHIS2 calls for all required APIs using dynamic DHIS2 code
     */
    private Map<IntegratedApi, Map<String, Object>> executeDhis2Calls(
            Set<IntegratedApi> apis, String periodParam, String dhis2Code) {

        Map<IntegratedApi, Map<String, Object>> responses = new HashMap<>();

        for (IntegratedApi api : apis) {
            try {
                log.info("Executing call for API: {} ({})", api.getCode(), api.getType());

                // Pass the dynamic dhis2Code to the client
                Map<String, Object> response = dhis2Client.executeApiCall(api, periodParam, dhis2Code);
                responses.put(api, response);

            } catch (Exception e) {
                log.error("Failed to execute API call for {}: {}", api.getCode(), e.getMessage());
                responses.put(api, new HashMap<>()); // store empty response to continue processing
            }
        }

        return responses;
    }

    /**
     * Build middleware response from API responses and mappings
     */
    private MiddlewareResponseDto buildMiddlewareResponse(
            Map<IntegratedApi, List<IntegrationMapping>> mappingsByApi,
            Map<IntegratedApi, Map<String, Object>> apiResponses,
            Map<String, Map<String, Object>> orgUnitsMetadata) {

        Map<AggregationKey, MiddlewareRowDto> aggregatedRows = new LinkedHashMap<>();

        for (Map.Entry<IntegratedApi, List<IntegrationMapping>> entry : mappingsByApi.entrySet()) {
            IntegratedApi api = entry.getKey();
            List<IntegrationMapping> apiMappings = entry.getValue();
            Map<String, Object> response = apiResponses.get(api);

            if (response == null || response.isEmpty()) continue;

            if (api.getType() == IntegratedApi.ApiType.ANALYTICS) {
                AnalyticsResponseDto analytics = (AnalyticsResponseDto) response.get("data");
                AnalyticsDataExtractor extractor = new AnalyticsDataExtractor(analytics);

                for (IntegrationMapping mapping : apiMappings) {
                    processMapping(mapping, extractor, aggregatedRows, orgUnitsMetadata);
                }
            }
        }

        List<MiddlewareRowDto> rows = new ArrayList<>(aggregatedRows.values());
        log.info("Built middleware response with {} rows", rows.size());

        return MiddlewareResponseDto.builder()
                .rows(rows)
                .build();
    }

    private void processMapping(IntegrationMapping mapping,
                                AnalyticsDataExtractor extractor,
                                Map<AggregationKey, MiddlewareRowDto> aggregatedRows,
                                Map<String, Map<String, Object>> orgUnitsMetadata) {

        Set<String> orgUnits = extractor.getAllOrgUnits();
        Set<String> periods = extractor.getAllPeriods();

        for (String ou : orgUnits) {
            for (String period : periods) {
                AggregationKey key = new AggregationKey(ou, period);

                MiddlewareRowDto row = aggregatedRows.computeIfAbsent(key, k -> {
                    // Get enriched orgUnit details
                    Map<String, Object> ouDetails = orgUnitsMetadata.getOrDefault(ou, new HashMap<>());

                    return MiddlewareRowDto.builder()
                            .ou(ou)
                            .ouName(extractor.getOrgUnitName(ou))
                            .ouDetails(ouDetails)
                            .period(extractor.getPeriodName(period))
                            .attributes(new ArrayList<>())
                            .build();
                });

                Object value = extractValue(mapping, extractor, ou, period);
                if (value != null) {
                    AttributeDto attribute = AttributeDto.builder()
                            .name(mapping.getExternalKey())
                            .value(value)
                            .build();
                    row.getAttributes().add(attribute);
                }
            }
        }
    }

    private Object extractValue(IntegrationMapping mapping, AnalyticsDataExtractor extractor,
                                String ou, String period) {

        switch (mapping.getMappingType()) {
            case DATA_ELEMENT:
                return extractor.getDataElementValue(mapping.getData(), ou, period);

            case DATA_ELEMENT_WITH_DISAGGREGATION:
                String[] parts = mapping.getData().split("\\.");
                if (parts.length == 2) {
                    return extractor.getDisaggregatedValue(parts[0], parts[1], ou, period);
                }
                return null;

            case INDICATOR:
                return extractor.getIndicatorValue(mapping.getData(), ou, period);

            case DATA_ELEMENT_WITH_DISAGGREGATION_AND_ATTRIBUTE:
                String[] attrParts = mapping.getData().split("\\.");
                if (attrParts.length >= 2) {
                    String deId = attrParts[0];
                    String attributeValue = attrParts.length == 3 ? attrParts[2] : attrParts[1];
                    return extractor.getAttributedValue(deId, attributeValue, ou, period);
                }
                return null;

            default:
                log.warn("Unsupported mapping type: {}", mapping.getMappingType());
                return null;
        }
    }

    // Inner classes remain the same
    private static class AggregationKey {
        private final String ou;
        private final String period;

        public AggregationKey(String ou, String period) {
            this.ou = ou;
            this.period = period;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AggregationKey)) return false;
            AggregationKey that = (AggregationKey) o;
            return Objects.equals(ou, that.ou) &&
                    Objects.equals(period, that.period);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ou, period);
        }
    }

    private class AnalyticsDataExtractor {
        private final AnalyticsResponseDto analytics;
        private final Map<String, Integer> headerIndexes;
        private final Map<String, Map<String, Map<String, Object>>> valueIndex;

        public AnalyticsDataExtractor(AnalyticsResponseDto analytics) {
            this.analytics = analytics;
            this.headerIndexes = buildHeaderIndexes();
            this.valueIndex = buildValueIndex();
        }

        private Map<String, Integer> buildHeaderIndexes() {
            Map<String, Integer> indexes = new HashMap<>();
            List<String> headerNames = analytics.getHeaderNames();
            if (headerNames != null) {
                for (int i = 0; i < headerNames.size(); i++) {
                    indexes.put(headerNames.get(i), i);
                }
            }
            return indexes;
        }

        private Map<String, Map<String, Map<String, Object>>> buildValueIndex() {
            Map<String, Map<String, Map<String, Object>>> index = new HashMap<>();
            Integer dxIdx = headerIndexes.get("dx");
            Integer ouIdx = headerIndexes.get("ou");
            Integer peIdx = headerIndexes.get("pe");
            Integer valueIdx = headerIndexes.get("value");

            if (analytics.getRows() != null) {
                for (List<Object> row : analytics.getRows()) {
                    String dx = dxIdx != null && dxIdx < row.size() ? (String) row.get(dxIdx) : null;
                    String ou = ouIdx != null && ouIdx < row.size() ? (String) row.get(ouIdx) : null;
                    String pe = peIdx != null && peIdx < row.size() ? (String) row.get(peIdx) : null;
                    Object value = valueIdx != null && valueIdx < row.size() ? row.get(valueIdx) : null;

                    if (dx != null && ou != null) {
                        index.computeIfAbsent(dx, k -> new HashMap<>())
                                .computeIfAbsent(ou, k -> new HashMap<>())
                                .put(pe != null ? pe : "default", value);
                    }
                }
            }

            return index;
        }

        public Set<String> getAllPeriods() {
            if (analytics.getMetaData() != null &&
                    analytics.getMetaData().getDimensions() != null &&
                    analytics.getMetaData().getDimensions().getPe() != null) {
                return new HashSet<>(analytics.getMetaData().getDimensions().getPe());
            }
            return new HashSet<>();
        }

        public Set<String> getAllOrgUnits() {
            if (analytics.getMetaData() != null &&
                    analytics.getMetaData().getDimensions() != null &&
                    analytics.getMetaData().getDimensions().getOu() != null) {
                return new HashSet<>(analytics.getMetaData().getDimensions().getOu());
            }
            return new HashSet<>();
        }

        public String getOrgUnitName(String ouId) {
            return dhis2Client.getOrgUnitName(analytics, ouId);
        }

        public String getPeriodName(String periodId) {
            return dhis2Client.getPeriodName(analytics, periodId);
        }

        public Object getDataElementValue(String deId, String ou, String period) {
            return getValueFromIndex(deId, ou, period);
        }

        public Object getDisaggregatedValue(String deId, String cocId, String ou, String period) {
            String key = deId + "." + cocId;
            return getValueFromIndex(key, ou, period);
        }

        public Object getIndicatorValue(String indicatorId, String ou, String period) {
            return getValueFromIndex(indicatorId, ou, period);
        }

        public Object getAttributedValue(String deId, String attributeValue, String ou, String period) {
            log.debug("Extracting attributed value - DE: {}, Attribute: {}, OU: {}, Period: {}",
                    deId, attributeValue, ou, period);

            if (analytics.getRows() == null || analytics.getRows().isEmpty()) {
                log.debug("No rows available for extraction");
                return null;
            }

            Integer dxIdx = headerIndexes.get("dx");
            Integer ouIdx = headerIndexes.get("ou");
            Integer peIdx = headerIndexes.get("pe");
            Integer valueIdx = headerIndexes.get("value");

            Integer attrIdx = null;
            String attrHeaderName = null;

            for (Map.Entry<String, Integer> entry : headerIndexes.entrySet()) {
                String headerName = entry.getKey();
                if (!headerName.equals("dx") && !headerName.equals("ou") &&
                        !headerName.equals("pe") && !headerName.equals("value") &&
                        !headerName.equals("numerator") && !headerName.equals("denominator") &&
                        !headerName.equals("factor") && !headerName.equals("multiplier") &&
                        !headerName.equals("divisor")) {
                    attrIdx = entry.getValue();
                    attrHeaderName = headerName;
                    log.debug("Detected attribute dimension header: {} at index {}", headerName, attrIdx);
                    break;
                }
            }

            if (attrIdx == null) {
                log.warn("No attribute dimension found in headers for attributed value extraction");
                return getValueFromIndex(deId, ou, period);
            }

            for (List<Object> row : analytics.getRows()) {
                try {
                    String rowDx = dxIdx != null && dxIdx < row.size() ? (String) row.get(dxIdx) : null;
                    String rowOu = ouIdx != null && ouIdx < row.size() ? (String) row.get(ouIdx) : null;
                    String rowPe = peIdx != null && peIdx < row.size() ? (String) row.get(peIdx) : null;
                    String rowAttr = attrIdx < row.size() ? (String) row.get(attrIdx) : null;
                    Object rowValue = valueIdx != null && valueIdx < row.size() ? row.get(valueIdx) : null;

                    boolean dxMatch = deId.equals(rowDx);
                    boolean ouMatch = ou.equals(rowOu);
                    boolean peMatch = period.equals(rowPe) || (rowPe == null && "default".equals(period));
                    boolean attrMatch = attributeValue.equals(rowAttr);

                    if (dxMatch && ouMatch && peMatch && attrMatch) {
                        log.debug("Found matching row - Value: {}", rowValue);
                        return rowValue;
                    }

                } catch (Exception e) {
                    log.warn("Error processing row for attributed value: {}", e.getMessage());
                    continue;
                }
            }

            log.debug("No matching row found for attributed value extraction");
            return null;
        }

        private Object getValueFromIndex(String dx, String ou, String period) {
            if (valueIndex.containsKey(dx) &&
                    valueIndex.get(dx).containsKey(ou)) {
                Map<String, Object> periods = valueIndex.get(dx).get(ou);
                return periods.getOrDefault(period, periods.get("default"));
            }
            return null;
        }
    }
}