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
 * Each API uses its own integratedSystem instead of the global _dhis2Code
 *
 * UPDATED: Groups data by OU only, periods are nested inside each OU
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MiddlewareProcessorService {

    private final IntegrationMappingRepository mappingRepository;
    private final Dhis2ClientService dhis2Client;

    /**
     * Process middleware request
     * _dhis2Code is now IGNORED - each API uses its own integratedSystem
     */
    @Transactional(readOnly = true)
    public MiddlewareResponseDto processMiddlewareRequest(
            String dynamicRouteId,
            String periodParam,
            String ouParam,
            String dhis2Code) {
        log.info("Processing middleware request for Route ID: {} with period: {}, ou: {}",
                dynamicRouteId, periodParam, ouParam);

        try {
            List<IntegrationMapping> mappings = mappingRepository
                    .findActiveRouteMappings(dynamicRouteId);

            if (mappings.isEmpty()) {
                log.warn("No active mappings found for Route ID: {}", dynamicRouteId);
                return MiddlewareResponseDto.builder()
                        .rows(new ArrayList<>())
                        .build();
            }

            // Step 2: Get ou parameter from request if available
            String ouFromRequest = ouParam;
            log.debug("Request parameters - ou: {}, pe: {}", ouFromRequest, periodParam);

            // Step 3: Group mappings by API
            Map<IntegratedApi, List<IntegrationMapping>> mappingsByApi =
                    groupMappingsByApi(mappings);

            // Step 4: Fetch org units metadata for each unique integratedSystem
            Map<String, Map<String, Map<String, Object>>> orgUnitsMetadataBySystem =
                    fetchOrgUnitsForAllSystems(mappingsByApi.keySet(), dynamicRouteId);

            // Step 5: Execute DHIS2 calls - each API uses its own integratedSystem
            Map<IntegratedApi, Map<String, Object>> apiResponses =
                    executeDhis2CallsWithDynamicParams(
                            mappingsByApi.keySet(),
                            periodParam,
                            ouFromRequest
                    );

            // Step 6: Build response with enriched orgUnit data (GROUPED BY OU)
            return buildMiddlewareResponse(
                    mappingsByApi,
                    apiResponses,
                    orgUnitsMetadataBySystem
            );

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing middleware request: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process middleware request: " + e.getMessage());
        }
    }

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
     * Fetch org units metadata for all unique integrated systems
     */
    private Map<String, Map<String, Map<String, Object>>> fetchOrgUnitsForAllSystems(
            Set<IntegratedApi> apis,
            String middlewareApiName) {

        Map<String, Map<String, Map<String, Object>>> metadataBySystem = new HashMap<>();

        for (IntegratedApi api : apis) {
            String systemCode = api.getIntegratedSystem();

            if (!metadataBySystem.containsKey(systemCode)) {
                log.info("Fetching org units metadata using integratedSystem: {}", systemCode);

                Map<String, Map<String, Object>> orgUnits =
                        dhis2Client.fetchOrganisationUnitsMetadata(
                                systemCode,
                                middlewareApiName
                        );

                metadataBySystem.put(systemCode, orgUnits);
            }
        }

        return metadataBySystem;
    }

    /**
     * Execute DHIS2 calls - each API uses its own integratedSystem
     */
    private Map<IntegratedApi, Map<String, Object>> executeDhis2CallsWithDynamicParams(
            Set<IntegratedApi> apis,
            String periodParam,
            String ouFromRequest) {

        Map<IntegratedApi, Map<String, Object>> responses = new HashMap<>();

        for (IntegratedApi api : apis) {
            try {
                // Use the API's own integratedSystem
                String systemCode = api.getIntegratedSystem();

                log.info("Executing call for API: {} ({}) using integratedSystem: {}",
                        api.getCode(), api.getType(), systemCode);
                log.debug("API flags - useOuFromRequest: {}, usePeFromRequest: {}",
                        api.getUseOuFromRequest(), api.getUsePeFromRequest());

                // Validate pe if required
                if (api.getUsePeFromRequest() && (periodParam == null || periodParam.trim().isEmpty())) {
                    throw new IllegalArgumentException(
                            "Parameter 'pe' is required for API: " + api.getCode() +
                                    " (configured to use pe from request)"
                    );
                }

                // Validate ou if required
                if (api.getUseOuFromRequest() && (ouFromRequest == null || ouFromRequest.trim().isEmpty())) {
                    throw new IllegalArgumentException(
                            "Parameter 'ou' is required for API: " + api.getCode() +
                                    " (configured to use ou from request)"
                    );
                }

                // Execute call using the API's integratedSystem (NOT the global _dhis2Code)
                Map<String, Object> response = dhis2Client.executeApiCall(
                        api,
                        periodParam,
                        systemCode,
                        ouFromRequest,
                        periodParam,
                        api.getUseOuFromRequest(),
                        api.getUsePeFromRequest()
                );

                responses.put(api, response);

            } catch (Exception e) {
                log.error("Failed to execute API call for {}: {}", api.getCode(), e.getMessage());
                responses.put(api, new HashMap<>());
            }
        }

        return responses;
    }

    /**
     * Build response - GROUPED BY OU ONLY
     * ouDetails appear once per OU, periods are nested inside
     */
    private MiddlewareResponseDto buildMiddlewareResponse(
            Map<IntegratedApi, List<IntegrationMapping>> mappingsByApi,
            Map<IntegratedApi, Map<String, Object>> apiResponses,
            Map<String, Map<String, Map<String, Object>>> orgUnitsMetadataBySystem) {

        // Changed: Now we group by OU only (no period in key)
        Map<String, MiddlewareRowDto> aggregatedByOu = new LinkedHashMap<>();

        for (Map.Entry<IntegratedApi, List<IntegrationMapping>> entry : mappingsByApi.entrySet()) {
            IntegratedApi api = entry.getKey();
            List<IntegrationMapping> apiMappings = entry.getValue();
            Map<String, Object> response = apiResponses.get(api);

            if (response == null || response.isEmpty()) continue;

            // Get org units metadata for this API's system
            String systemCode = api.getIntegratedSystem();
            Map<String, Map<String, Object>> orgUnitsMetadata =
                    orgUnitsMetadataBySystem.getOrDefault(systemCode, new HashMap<>());

            if (api.getType() == IntegratedApi.ApiType.ANALYTICS) {
                AnalyticsResponseDto analytics = (AnalyticsResponseDto) response.get("data");
                AnalyticsDataExtractor extractor = new AnalyticsDataExtractor(analytics);

                for (IntegrationMapping mapping : apiMappings) {
                    processMappingGroupedByOu(mapping, extractor, aggregatedByOu, orgUnitsMetadata);
                }
            }
        }

        List<MiddlewareRowDto> rows = new ArrayList<>(aggregatedByOu.values());
        log.info("Built middleware response with {} org units", rows.size());

        return MiddlewareResponseDto.builder()
                .rows(rows)
                .build();
    }

    /**
     * NEW: Process mapping and group by OU only
     * Each OU will have a list of periods with their attributes
     */
    private void processMappingGroupedByOu(IntegrationMapping mapping,
                                           AnalyticsDataExtractor extractor,
                                           Map<String, MiddlewareRowDto> aggregatedByOu,
                                           Map<String, Map<String, Object>> orgUnitsMetadata) {

        Set<String> orgUnits = extractor.getAllOrgUnits();
        Set<String> periods = extractor.getAllPeriods();

        for (String ou : orgUnits) {
            // Get or create the OU row (ouDetails appear only once here)
            MiddlewareRowDto row = aggregatedByOu.computeIfAbsent(ou, k -> {
                Map<String, Object> ouDetails = orgUnitsMetadata.getOrDefault(ou, new HashMap<>());

                return MiddlewareRowDto.builder()
                        .ou(ou)
                        .ouName(extractor.getOrgUnitName(ou))
                        .ouDetails(ouDetails)
                        .periods(new ArrayList<>())
                        .build();
            });

            // Now loop through periods and add them to this OU
            for (String period : periods) {
                List<AttributeDto> attributes = extractValuesWithAttributes(
                        mapping, extractor, ou, period
                );

                // Create a period entry
                PeriodDataDto periodData = PeriodDataDto.builder()
                        .period(extractor.getPeriodName(period))
                        .attributes(attributes)
                        .build();

                // Check if this period already exists for this OU
                boolean periodExists = row.getPeriods().stream()
                        .anyMatch(p -> p.getPeriod().equals(periodData.getPeriod()));

                if (periodExists) {
                    // Merge attributes into existing period
                    row.getPeriods().stream()
                            .filter(p -> p.getPeriod().equals(periodData.getPeriod()))
                            .findFirst()
                            .ifPresent(existingPeriod ->
                                    existingPeriod.getAttributes().addAll(attributes)
                            );
                } else {
                    // Add new period
                    row.getPeriods().add(periodData);
                }
            }
        }
    }

    private List<AttributeDto> extractValuesWithAttributes(
            IntegrationMapping mapping,
            AnalyticsDataExtractor extractor,
            String ou,
            String period) {

        List<AttributeDto> results = new ArrayList<>();

        if (extractor.hasAttributeDimension()) {
            Map<String, Object> attributeValues = extractor.getValuesWithAttribute(
                    mapping.getData(), ou, period
            );

            for (Map.Entry<String, Object> entry : attributeValues.entrySet()) {
                String attributeId = entry.getKey();
                Object value = entry.getValue();
                String attName = extractor.getAttributeName(attributeId);

                AttributeDto attr = new AttributeDto(
                        mapping.getExternalKey(),
                        value,
                        attName
                );

                results.add(attr);
            }
        } else {
            Object value = extractValue(mapping, extractor, ou, period);
            if (value != null) {
                AttributeDto attr = new AttributeDto(
                        mapping.getExternalKey(),
                        value
                );

                results.add(attr);
            }
        }

        return results;
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

            default:
                log.warn("Unsupported mapping type: {}", mapping.getMappingType());
                return null;
        }
    }

    private class AnalyticsDataExtractor {
        private final AnalyticsResponseDto analytics;
        private final Map<String, Integer> headerIndexes;
        private final Map<String, Map<String, Map<String, Object>>> valueIndex;
        private final Integer attributeIndex;
        private final String attributeDimensionName;

        public AnalyticsDataExtractor(AnalyticsResponseDto analytics) {
            this.analytics = analytics;
            this.headerIndexes = buildHeaderIndexes();
            this.attributeIndex = findAttributeIndex();
            this.attributeDimensionName = findAttributeDimensionName();
            this.valueIndex = buildValueIndex();

            if (hasAttributeDimension()) {
                log.info("Detected attribute dimension: {} at index {}",
                        attributeDimensionName, attributeIndex);
            }
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

        private Integer findAttributeIndex() {
            for (Map.Entry<String, Integer> entry : headerIndexes.entrySet()) {
                String headerName = entry.getKey();
                if (!headerName.equals("dx") && !headerName.equals("ou") &&
                        !headerName.equals("pe") && !headerName.equals("value") &&
                        !headerName.equals("numerator") && !headerName.equals("denominator") &&
                        !headerName.equals("factor") && !headerName.equals("multiplier") &&
                        !headerName.equals("divisor")) {
                    return entry.getValue();
                }
            }
            return null;
        }

        private String findAttributeDimensionName() {
            if (attributeIndex == null) return null;

            for (Map.Entry<String, Integer> entry : headerIndexes.entrySet()) {
                if (entry.getValue().equals(attributeIndex)) {
                    return entry.getKey();
                }
            }
            return null;
        }

        public boolean hasAttributeDimension() {
            return attributeIndex != null && attributeDimensionName != null;
        }

        public String getAttributeName(String attributeId) {
            if (analytics.getMetaData() != null &&
                    analytics.getMetaData().getItems() != null) {
                ItemDto item = analytics.getMetaData().getItems().get(attributeId);
                return item != null ? item.getName() : attributeId;
            }
            return attributeId;
        }

        public Map<String, Object> getValuesWithAttribute(String dx, String ou, String period) {
            Map<String, Object> result = new HashMap<>();

            if (!hasAttributeDimension() || analytics.getRows() == null) {
                return result;
            }

            Integer dxIdx = headerIndexes.get("dx");
            Integer ouIdx = headerIndexes.get("ou");
            Integer peIdx = headerIndexes.get("pe");
            Integer valueIdx = headerIndexes.get("value");

            for (List<Object> row : analytics.getRows()) {
                try {
                    String rowDx = dxIdx != null && dxIdx < row.size() ? (String) row.get(dxIdx) : null;
                    String rowOu = ouIdx != null && ouIdx < row.size() ? (String) row.get(ouIdx) : null;
                    String rowPe = peIdx != null && peIdx < row.size() ? (String) row.get(peIdx) : null;
                    String rowAttr = attributeIndex < row.size() ? (String) row.get(attributeIndex) : null;
                    Object rowValue = valueIdx != null && valueIdx < row.size() ? row.get(valueIdx) : null;

                    boolean dxMatches = matchesDx(dx, rowDx);

                    if (dxMatches && ou.equals(rowOu) && period.equals(rowPe) && rowAttr != null) {
                        result.put(rowAttr, rowValue);
                    }
                } catch (Exception e) {
                    log.warn("Error processing row with attribute: {}", e.getMessage());
                }
            }

            return result;
        }

        private boolean matchesDx(String searchDx, String rowDx) {
            if (searchDx == null || rowDx == null) return false;

            if (searchDx.equals(rowDx)) {
                return true;
            }

            if (searchDx.contains(".") && rowDx.startsWith(searchDx.split("\\.")[0])) {
                return true;
            }

            return false;
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