package com.middleware.backend.kaotocamel.service;

import com.middleware.backend.kaotocamel.dto.*;
import com.middleware.backend.kaotocamel.model.*;
import com.middleware.backend.kaotocamel.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Main service for processing middleware API requests
 * UPDATED: Groups data by OU → Period → AttName
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MiddlewareProcessorService {

    private final IntegrationMappingRepository mappingRepository;
    private final Dhis2ClientService dhis2Client;
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

            validateRequiredParameters(mappings, periodParam, ouParam);

            String ouFromRequest = ouParam;
            log.debug("Request parameters - ou: {}, pe: {}", ouFromRequest, periodParam);

            Map<IntegratedApi, List<IntegrationMapping>> mappingsByApi =
                    groupMappingsByApi(mappings);

            Map<String, Map<String, Map<String, Object>>> orgUnitsMetadataBySystem =
                    fetchOrgUnitsForAllSystems(mappingsByApi.keySet(), dynamicRouteId);

            Map<IntegratedApi, Map<String, Object>> apiResponses =
                    executeDhis2CallsWithDynamicParams(
                            mappingsByApi.keySet(),
                            periodParam,
                            ouFromRequest
                    );

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

    private void validateRequiredParameters(
            List<IntegrationMapping> mappings,
            String periodParam,
            String ouParam) {

         Set<IntegratedApi> apis = mappings.stream()
                .map(IntegrationMapping::getIntegratedApi)
                .collect(Collectors.toSet());

        List<String> missingParams = new ArrayList<>();

        for (IntegratedApi api : apis) {
             if (api.getUseOuFromRequest() != null && api.getUseOuFromRequest()) {
                if (ouParam == null || ouParam.trim().isEmpty()) {
                    missingParams.add("Parameter 'ou' (Organization Unit)");
                }
            }

             if (api.getUsePeFromRequest() != null && api.getUsePeFromRequest()) {
                if (periodParam == null || periodParam.trim().isEmpty()) {
                    missingParams.add("Parameter 'pe' (Period)");
                }
            }
        }

         if (!missingParams.isEmpty()) {
             List<String> uniqueParams = missingParams.stream().distinct().collect(Collectors.toList());
            String errorMessage = "Missing Required Parameters: " + String.join(", ", uniqueParams) + ". Please provide the missing parameters in your request.";

            log.warn("Missing required parameters: {}", errorMessage);
            throw new IllegalArgumentException(errorMessage);
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

    private Map<IntegratedApi, Map<String, Object>> executeDhis2CallsWithDynamicParams(
            Set<IntegratedApi> apis,
            String periodParam,
            String ouFromRequest) {

        Map<IntegratedApi, Map<String, Object>> responses = new HashMap<>();

        for (IntegratedApi api : apis) {
            try {
                String systemCode = api.getIntegratedSystem();

                log.info("Executing call for API: {} ({}) using integratedSystem: {}",
                        api.getCode(), api.getType(), systemCode);
                log.debug("API flags - useOuFromRequest: {}, usePeFromRequest: {}",
                        api.getUseOuFromRequest(), api.getUsePeFromRequest());

                if (api.getUsePeFromRequest() && (periodParam == null || periodParam.trim().isEmpty())) {
                    throw new IllegalArgumentException(
                            "Parameter 'pe' is required for API: " + api.getCode() +
                                    " (configured to use pe from request)"
                    );
                }

                if (api.getUseOuFromRequest() && (ouFromRequest == null || ouFromRequest.trim().isEmpty())) {
                    throw new IllegalArgumentException(
                            "Parameter 'ou' is required for API: " + api.getCode() +
                                    " (configured to use ou from request)"
                    );
                }

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
     * Build response - GROUPED BY OU → PERIOD → ATTNAME
     */
    private MiddlewareResponseDto buildMiddlewareResponse(
            Map<IntegratedApi, List<IntegrationMapping>> mappingsByApi,
            Map<IntegratedApi, Map<String, Object>> apiResponses,
            Map<String, Map<String, Map<String, Object>>> orgUnitsMetadataBySystem) {

        Map<String, MiddlewareRowDto> aggregatedByOu = new LinkedHashMap<>();

        for (Map.Entry<IntegratedApi, List<IntegrationMapping>> entry : mappingsByApi.entrySet()) {
            IntegratedApi api = entry.getKey();
            List<IntegrationMapping> apiMappings = entry.getValue();
            Map<String, Object> response = apiResponses.get(api);

            if (response == null || response.isEmpty()) continue;

            String systemCode = api.getIntegratedSystem();
            Map<String, Map<String, Object>> orgUnitsMetadata =
                    orgUnitsMetadataBySystem.getOrDefault(systemCode, new HashMap<>());

            if (api.getType() == IntegratedApi.ApiType.ANALYTICS) {
                AnalyticsResponseDto analytics = (AnalyticsResponseDto) response.get("data");
                
                // Fetch categoryOption codes from DHIS2 for attribute dimensions
                Set<String> attributeIds = dhis2Client.extractAttributeIdsFromApiUrl(api.getApiUrl());
                Map<String, String> categoryOptionCodes = new HashMap<>();
                if (!attributeIds.isEmpty()) {
                    categoryOptionCodes = dhis2Client.fetchCategoryOptionCodes(systemCode, attributeIds);
                    log.debug("Fetched {} categoryOption codes for API: {}", categoryOptionCodes.size(), api.getCode());
                }
                
                AnalyticsDataExtractor extractor = new AnalyticsDataExtractor(analytics, categoryOptionCodes);

                for (IntegrationMapping mapping : apiMappings) {
                    processMappingGroupedByOuPeriodAndAttName(
                            mapping, extractor, aggregatedByOu, orgUnitsMetadata
                    );
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
     * Process mapping and group by OU → PERIOD → ATTNAME
     */
    private void processMappingGroupedByOuPeriodAndAttName(
            IntegrationMapping mapping,
            AnalyticsDataExtractor extractor,
            Map<String, MiddlewareRowDto> aggregatedByOu,
            Map<String, Map<String, Object>> orgUnitsMetadata) {

        Set<String> orgUnits = extractor.getAllOrgUnits();
        Set<String> periods = extractor.getAllPeriods();

        for (String ou : orgUnits) {
            // Get or create the OU row
            MiddlewareRowDto row = aggregatedByOu.computeIfAbsent(ou, k -> {
                Map<String, Object> ouDetails = orgUnitsMetadata.getOrDefault(ou, new HashMap<>());

                return MiddlewareRowDto.builder()
                        .ou(ou)
                        .ouName(extractor.getOrgUnitName(ou))
                        .ouDetails(ouDetails)
                        .periods(new ArrayList<>())
                        .build();
            });

            // Loop through periods
            for (String period : periods) {
                String periodName = extractor.getPeriodName(period);

                // Find or create period
                PeriodDataDto periodData = row.getPeriods().stream()
                        .filter(p -> p.getPeriod().equals(periodName))
                        .findFirst()
                        .orElseGet(() -> {
                            PeriodDataDto newPeriod = PeriodDataDto.builder()
                                    .period(periodName)
                                    .attributeGroups(new ArrayList<>())
                                    .build();
                            row.getPeriods().add(newPeriod);
                            return newPeriod;
                        });

                // Extract attributes with their attName
                List<AttributeWithAttName> attributesWithAttName =
                        extractValuesWithAttributesAndAttName(mapping, extractor, ou, period);

                // Group attributes by attName - keep attCode info
                Map<String, AttributeGroupInfo> groupedByAttName = new LinkedHashMap<>();
                
                for (AttributeWithAttName attr : attributesWithAttName) {
                    String attName = attr.attName != null ? attr.attName : "default";
                    String attCode = attr.attCode;
                    
                    groupedByAttName.computeIfAbsent(attName, k -> 
                        new AttributeGroupInfo(attName, attCode)
                    ).attributes.add(attr.attribute);
                }

                // Add or merge attribute groups
                for (AttributeGroupInfo groupInfo : groupedByAttName.values()) {
                    String attName = groupInfo.attName;
                    String attCode = groupInfo.attCode;
                    List<AttributeDto> attributes = groupInfo.attributes;

                    // Find existing attribute group or create new one
                    AttributeGroupDto existingGroup = periodData.getAttributeGroups().stream()
                            .filter(g -> g.getAttName().equals(attName))
                            .findFirst()
                            .orElse(null);

                    if (existingGroup != null) {
                        // Merge into existing group
                        existingGroup.getAttributes().addAll(attributes);
                    } else {
                        // Create new group with code
                        AttributeGroupDto newGroup = AttributeGroupDto.builder()
                                .attName(attName)
                                .code(attCode)
                                .attributes(new ArrayList<>(attributes))
                                .build();
                        periodData.getAttributeGroups().add(newGroup);
                    }
                }
            }
        }
    }

    /**
     * Helper class to temporarily store attribute with its attName and attCode
     */
    private static class AttributeWithAttName {
        AttributeDto attribute;
        String attName;
        String attCode;

        public AttributeWithAttName(AttributeDto attribute, String attName, String attCode) {
            this.attribute = attribute;
            this.attName = attName;
            this.attCode = attCode;
        }
    }

    /**
     * Helper class to group attributes by attName and preserve the code
     */
    private static class AttributeGroupInfo {
        String attName;
        String attCode;
        List<AttributeDto> attributes;

        public AttributeGroupInfo(String attName, String attCode) {
            this.attName = attName;
            this.attCode = attCode;
            this.attributes = new ArrayList<>();
        }
    }

    /**
     * Extract values with attributes and preserve attName information
     */
    private List<AttributeWithAttName> extractValuesWithAttributesAndAttName(
            IntegrationMapping mapping,
            AnalyticsDataExtractor extractor,
            String ou,
            String period) {

        List<AttributeWithAttName> results = new ArrayList<>();

        if (extractor.hasAttributeDimension()) {
            // Has attribute dimension - extract with attName and attCode
            Map<String, Object> attributeValues = extractor.getValuesWithAttribute(
                    mapping.getData(), ou, period
            );

            for (Map.Entry<String, Object> entry : attributeValues.entrySet()) {
                String attributeId = entry.getKey();
                Object value = entry.getValue();
                String attName = extractor.getAttributeName(attributeId);
                String attCode = extractor.getAttributeCode(attributeId);

                AttributeDto attr = new AttributeDto(
                        mapping.getExternalKey(),
                        value
                );

                results.add(new AttributeWithAttName(attr, attName, attCode));
            }
        } else {
            // No attribute dimension - use default
            Object value = extractValue(mapping, extractor, ou, period);
            if (value != null) {
                AttributeDto attr = new AttributeDto(
                        mapping.getExternalKey(),
                        value
                );

                results.add(new AttributeWithAttName(attr, "default", null));
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
        private final Map<String, String> categoryOptionCodes; // ID -> CODE mapping from DHIS2

        public AnalyticsDataExtractor(AnalyticsResponseDto analytics, Map<String, String> categoryOptionCodes) {
            this.analytics = analytics;
            this.categoryOptionCodes = categoryOptionCodes != null ? categoryOptionCodes : new HashMap<>();
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

        public String getAttributeCode(String attributeId) {
            // First, try to get code from categoryOptionCodes map (fetched from DHIS2)
            if (categoryOptionCodes.containsKey(attributeId)) {
                return categoryOptionCodes.get(attributeId);
            }
            // Fallback to metaData.items (usually doesn't have code)
            if (analytics.getMetaData() != null &&
                    analytics.getMetaData().getItems() != null) {
                ItemDto item = analytics.getMetaData().getItems().get(attributeId);
                if (item != null && item.getCode() != null) {
                    return item.getCode();
                }
            }
            // Return null if no code found (instead of ID)
            return null;
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