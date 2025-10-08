package com.middleware.backend.kaotocamel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.middleware.backend.kaotocamel.dto.*;
import com.middleware.backend.kaotocamel.model.*;
import com.middleware.backend.kaotocamel.repository.*;
import com.middleware.backend.kaotocamel.spec.IntegrationMappingSpecification;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntegrationMappingService {

    private final IntegrationMappingRepository mappingRepository;
    private final IntegratedApiRepository apiRepository;

    @Transactional
    public IntegrationMappingDto create(IntegrationMappingRequestDto request) {
        log.info("Creating integration mapping for API: {}", request.getMiddlewareApiName());
        IntegratedApi api = apiRepository.findById(request.getIntegratedApiId())
                .orElseThrow(() -> new RuntimeException("Integrated API not found: " + request.getIntegratedApiId()));
        IntegrationMapping entity = new IntegrationMapping();
        mapRequestToEntity(request, entity, api);
        IntegrationMapping saved = mappingRepository.save(entity);
        log.info("Created integration mapping: {} -> {}", saved.getMiddlewareApiName(), saved.getExternalKey());
        return mapEntityToDto(saved);
    }

    @Transactional
    public IntegrationMappingDto update(Long id, IntegrationMappingRequestDto request) {
        log.info("Updating integration mapping: {}", id);
        IntegrationMapping entity = mappingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Integration mapping not found: " + id));
        IntegratedApi api = entity.getIntegratedApi();
        if (!entity.getIntegratedApiId().equals(request.getIntegratedApiId())) {
            api = apiRepository.findById(request.getIntegratedApiId())
                    .orElseThrow(() -> new RuntimeException("Integrated API not found: " + request.getIntegratedApiId()));
        }
        mapRequestToEntity(request, entity, api);
        IntegrationMapping saved = mappingRepository.save(entity);
        log.info("Updated integration mapping: {}", saved.getId());
        return mapEntityToDto(saved);
    }

    @Transactional
    public void softDelete(Long id) {
        log.info("Soft deleting integration mapping: {}", id);
        IntegrationMapping entity = mappingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Integration mapping not found: " + id));
        entity.setIsActive(false);
        mappingRepository.save(entity);
        log.info("Soft deleted integration mapping: {}", id);
    }

    @Transactional(readOnly = true)
    public Optional<IntegrationMappingDto> findById(Long id) {
        return mappingRepository.findById(id)
                .map(this::mapEntityToDto);
    }

    @Transactional(readOnly = true)
    public Page<IntegrationMappingDto> findWithFilters(String middlewareApiName, Long integratedApiId,
                                                       String mappingType, String externalKey,
                                                       Boolean isActive, Pageable pageable) {
        Specification<IntegrationMapping> spec = Specification.where(null);
        if (middlewareApiName != null && !middlewareApiName.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("middlewareApiName"), middlewareApiName));
        }
        if (integratedApiId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("integratedApiId"), integratedApiId));
        }
        if (mappingType != null && !mappingType.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("mappingType"), IntegrationMapping.MappingType.valueOf(mappingType)));
        }
        if (externalKey != null && !externalKey.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("externalKey")), "%" + externalKey.toLowerCase() + "%"));
        }
        if (isActive != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("isActive"), isActive));
        }
        return mappingRepository.findAll(spec, pageable)
                .map(this::mapEntityToDto);
    }

    @Transactional(readOnly = true)
    public List<IntegrationMappingDto> findByMiddlewareApi(String middlewareApiName) {
        return mappingRepository.findByMiddlewareApiNameAndIsActiveTrue(middlewareApiName)
                .stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<IntegrationMappingDto> findWithAdvancedFilters(
            String middlewareApiName, Long integratedApiId, String integratedApiCode,
            String mappingType, String data, String externalKey, String attribute,
            Boolean isActive, String notes,
            LocalDateTime createdAfter, LocalDateTime createdBefore,
            LocalDateTime updatedAfter, LocalDateTime updatedBefore,
            String search, Long minId, Long maxId,
            Pageable pageable) {
        log.debug("Searching with advanced filters - middlewareApi: {}, integratedApiId: {}, mappingType: {}",
                middlewareApiName, integratedApiId, mappingType);
        Specification<IntegrationMapping> spec = IntegrationMappingSpecification.buildSpecification(
                middlewareApiName, integratedApiId, integratedApiCode, mappingType,
                data, externalKey, attribute, isActive, notes,
                createdAfter, createdBefore, updatedAfter, updatedBefore,
                search, minId, maxId
        );
        Page<IntegrationMappingDto> result = mappingRepository.findAll(spec, pageable)
                .map(this::mapEntityToDto);
        log.debug("Found {} results out of {} total", result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    public byte[] exportFile(Specification<IntegrationMapping> spec, Pageable pageable, String type) {
        List<IntegrationMapping> data;
        try {
            data = findAllForExport(spec, pageable);
        } catch (Exception e) {
            log.error("Error fetching data for export", e);
            data = Collections.emptyList();
        }
        try {
            if ("CSV".equalsIgnoreCase(type)) {
                return convertToCSV(data).getBytes(StandardCharsets.UTF_8);
            } else if ("Excel".equalsIgnoreCase(type) || "XLSX".equalsIgnoreCase(type)) {
                return convertToExcel(data);
            } else {
                throw new IllegalArgumentException("Unsupported export type: " + type);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to export file", e);
        }
    }

    @Transactional
    public Map<String, Object> importFromExcel(InputStream inputStream, boolean updateExisting) throws IOException {
        Map<String, Object> result = new HashMap<>();
        List<IntegrationMappingDto> imported = new ArrayList<>();
        List<IntegrationMappingDto> updated = new ArrayList<>();
        List<Map<String, Object>> ignored = new ArrayList<>();
        List<Map<String, Object>> errors = new ArrayList<>();
        int totalRows = 0;
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            int rowCount = sheet.getLastRowNum();
            totalRows = rowCount;
            log.info("Processing {} data rows from Excel (updateExisting={})", totalRows, updateExisting);
            for (int i = 1; i <= rowCount; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    log.debug("Row {} is null, skipping", i + 1);
                    continue;
                }
                try {
                    IntegrationMappingRequestDto request = parseRowToRequest(row);
                    boolean isEmptyRow = (request.getMiddlewareApiName() == null || request.getMiddlewareApiName().trim().isEmpty()) &&
                            (request.getExternalKey() == null || request.getExternalKey().trim().isEmpty()) &&
                            (request.getData() == null || request.getData().trim().isEmpty());
                    if (isEmptyRow) {
                        log.debug("Row {} is empty, skipping", i + 1);
                        continue;
                    }
                    Map<String, Object> validation = validateMappingForImport(request);
                    if (!(boolean) validation.get("valid")) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("row", i + 1);
                        error.put("middlewareApiName", request.getMiddlewareApiName());
                        error.put("externalKey", request.getExternalKey());
                        error.put("integratedApiId", request.getIntegratedApiId());
                        error.put("mappingType", request.getMappingType());
                        error.put("data", request.getData());
                        error.put("errors", validation.get("errors"));
                        errors.add(error);
                        log.warn("Row {} validation failed: {}", i + 1, validation.get("errors"));
                        continue;
                    }
                    Optional<IntegrationMapping> existing = mappingRepository
                            .findByMiddlewareApiNameAndExternalKey(
                                    request.getMiddlewareApiName(), request.getExternalKey());
                    if (existing.isPresent()) {
                        if (updateExisting) {
                            IntegrationMappingDto updatedDto = update(existing.get().getId(), request);
                            updated.add(updatedDto);
                            log.info("Row {}: Updated existing mapping - API: '{}', Key: '{}'",
                                    i + 1, request.getMiddlewareApiName(), request.getExternalKey());
                        } else {
                            Map<String, Object> ignoredItem = new HashMap<>();
                            ignoredItem.put("row", i + 1);
                            ignoredItem.put("middlewareApiName", request.getMiddlewareApiName());
                            ignoredItem.put("externalKey", request.getExternalKey());
                            ignoredItem.put("reason", "Already exists and update is disabled");
                            ignored.add(ignoredItem);
                            log.info("Row {}: Ignored existing mapping - API: '{}', Key: '{}'",
                                    i + 1, request.getMiddlewareApiName(), request.getExternalKey());
                        }
                    } else {
                        IntegrationMappingDto created = create(request);
                        imported.add(created);
                        log.info("Row {}: Created new mapping - API: '{}', Key: '{}'",
                                i + 1, request.getMiddlewareApiName(), request.getExternalKey());
                    }
                } catch (Exception e) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("row", i + 1);
                    error.put("error", "Unexpected error: " + e.getMessage());
                    error.put("errorType", e.getClass().getSimpleName());
                    errors.add(error);
                    log.error("Unexpected error processing row {}: {}", i + 1, e.getMessage(), e);
                }
            }
        }
        result.put("success", errors.isEmpty());
        result.put("totalRows", totalRows);
        result.put("imported", imported.size());
        result.put("updated", updated.size());
        result.put("ignored", ignored.size());
        result.put("failed", errors.size());
        result.put("newMappings", imported);
        result.put("updatedMappings", updated);
        result.put("ignoredMappings", ignored);
        result.put("errors", errors);
        log.info("Import completed: {} total, {} new, {} updated, {} ignored, {} failed",
                totalRows, imported.size(), updated.size(), ignored.size(), errors.size());
        return result;
    }

    private Map<String, Object> validateMappingForImport(IntegrationMappingRequestDto request) {
        Map<String, Object> validation = new HashMap<>();
        List<String> errors = new ArrayList<>();
        if (request.getMiddlewareApiName() == null || request.getMiddlewareApiName().trim().isEmpty()) {
            errors.add("Middleware API Name is required and cannot be empty");
        }
        if (request.getExternalKey() == null || request.getExternalKey().trim().isEmpty()) {
            errors.add("External Key is required and cannot be empty");
        }
        if (request.getIntegratedApiId() == null) {
            errors.add("Integrated API ID is required");
        } else {
            if (!apiRepository.existsById(request.getIntegratedApiId())) {
                errors.add("Integrated API not found with ID: " + request.getIntegratedApiId() +
                        ". Please check if this API exists in the system");
            }
        }
        if (request.getMappingType() == null || request.getMappingType().trim().isEmpty()) {
            errors.add("Mapping Type is required. Valid values: DATA_ELEMENT, DATA_ELEMENT_WITH_DISAGGREGATION, DATA_ELEMENT_WITH_DISAGGREGATION_AND_ATTRIBUTE, INDICATOR");
        } else {
            try {
                IntegrationMapping.MappingType type = IntegrationMapping.MappingType.valueOf(request.getMappingType());
                if (request.getData() == null || request.getData().trim().isEmpty()) {
                    errors.add("Data field is required and cannot be empty");
                } else {
                    switch (type) {
                        case DATA_ELEMENT:
                            if (request.getData().contains(".")) {
                                errors.add("DATA_ELEMENT should not contain disaggregation. Format: DE_UID (without dots). Current value: '" + request.getData() + "'");
                            }
                            break;
                        case DATA_ELEMENT_WITH_DISAGGREGATION:
                            if (!request.getData().contains(".")) {
                                errors.add("DATA_ELEMENT_WITH_DISAGGREGATION requires disaggregation. Format: DE_UID.COC_UID. Current value: '" + request.getData() + "'");
                            } else {
                                String[] parts = request.getData().split("\\.");
                                if (parts.length != 2) {
                                    errors.add("DATA_ELEMENT_WITH_DISAGGREGATION must have exactly 2 parts separated by dot. Format: DE_UID.COC_UID. Current value: '" + request.getData() + "'");
                                }
                            }
                            break;
                        case DATA_ELEMENT_WITH_DISAGGREGATION_AND_ATTRIBUTE:
                            if (!request.getData().contains(".")) {
                                errors.add("DATA_ELEMENT_WITH_DISAGGREGATION_AND_ATTRIBUTE requires disaggregation and attribute. Format: DE_UID.COC_UID.AOC_UID or DE_UID.AOC_UID. Current value: '" + request.getData() + "'");
                            } else {
                                String[] parts = request.getData().split("\\.");
                                if (parts.length < 2) {
                                    errors.add("DATA_ELEMENT_WITH_DISAGGREGATION_AND_ATTRIBUTE must have at least 2 parts separated by dots. Format: DE_UID.COC_UID.AOC_UID or DE_UID.AOC_UID. Current value: '" + request.getData() + "'");
                                }
                            }
                            break;
                        case INDICATOR:
                            break;
                        default:
                            errors.add("Unknown mapping type: " + type);
                            break;
                    }
                }
            } catch (IllegalArgumentException e) {
                errors.add("Invalid Mapping Type: '" + request.getMappingType() +
                        "'. Valid values are: DATA_ELEMENT, DATA_ELEMENT_WITH_DISAGGREGATION, DATA_ELEMENT_WITH_DISAGGREGATION_AND_ATTRIBUTE, INDICATOR");
            }
        }
        validation.put("valid", errors.isEmpty());
        validation.put("errors", errors);
        return validation;
    }

    private IntegrationMappingRequestDto parseRowToRequest(Row row) {
        IntegrationMappingRequestDto request = new IntegrationMappingRequestDto();
        request.setMiddlewareApiName(getCellValueAsString(row.getCell(0)));
        Double apiId = getCellValueAsDouble(row.getCell(1));
        if (apiId != null) {
            request.setIntegratedApiId(apiId.longValue());
        }
        request.setMappingType(getCellValueAsString(row.getCell(4)));
        request.setData(getCellValueAsString(row.getCell(5)));
        request.setAttribute(getCellValueAsString(row.getCell(6)));
        request.setExternalKey(getCellValueAsString(row.getCell(7)));
        String activeStr = getCellValueAsString(row.getCell(8));
        request.setIsActive(activeStr == null || activeStr.equalsIgnoreCase("ACTIVE"));
        request.setNotes(getCellValueAsString(row.getCell(9)));
        return request;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                double numValue = cell.getNumericCellValue();
                if (numValue == (long) numValue) {
                    return String.valueOf((long) numValue);
                }
                return String.valueOf(numValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return null;
        }
    }

    private Double getCellValueAsDouble(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    return Double.parseDouble(cell.getStringCellValue());
                } catch (NumberFormatException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    private List<IntegrationMapping> findAllForExport(Specification<IntegrationMapping> spec, Pageable pageable) {
        return mappingRepository.findAll((root, query, criteriaBuilder) -> {
            root.fetch("integratedApi", JoinType.LEFT);
            query.distinct(true);
            return spec != null ? spec.toPredicate(root, query, criteriaBuilder) : null;
        }, pageable).getContent();
    }

    private String convertToCSV(List<IntegrationMapping> records) {
        StringBuilder sb = new StringBuilder();
        sb.append("Middleware API Name,Integrated API ID,Integrated API Code,Integrated API Name,");
        sb.append("Mapping Type,Data,Attribute,External Key,Active,Notes\n");
        for (IntegrationMapping record : records) {
            sb.append(escapeCsv(record.getMiddlewareApiName())).append(",");
            sb.append(record.getIntegratedApiId()).append(",");
            sb.append(escapeCsv(record.getIntegratedApi() != null ? record.getIntegratedApi().getCode() : "")).append(",");
            sb.append(escapeCsv(record.getIntegratedApi() != null ? record.getIntegratedApi().getName() : "")).append(",");
            sb.append(record.getMappingType() != null ? record.getMappingType().toString() : "").append(",");
            sb.append(escapeCsv(record.getData())).append(",");
            sb.append(escapeCsv(record.getAttribute())).append(",");
            sb.append(escapeCsv(record.getExternalKey())).append(",");
            sb.append(record.getIsActive() ? "ACTIVE" : "INACTIVE").append(",");
            sb.append(escapeCsv(record.getNotes())).append("\n");
        }
        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private byte[] convertToExcel(List<IntegrationMapping> records) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Integration Mappings");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Middleware API Name");
            header.createCell(1).setCellValue("Integrated API ID");
            header.createCell(2).setCellValue("Integrated API Code");
            header.createCell(3).setCellValue("Integrated API Name");
            header.createCell(4).setCellValue("Mapping Type");
            header.createCell(5).setCellValue("Data");
            header.createCell(6).setCellValue("Attribute");
            header.createCell(7).setCellValue("External Key");
            header.createCell(8).setCellValue("Active");
            header.createCell(9).setCellValue("Notes");
            int rowIdx = 1;
            for (IntegrationMapping record : records) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(record.getMiddlewareApiName() != null ? record.getMiddlewareApiName() : "");
                row.createCell(1).setCellValue(record.getIntegratedApiId() != null ? record.getIntegratedApiId() : 0);
                row.createCell(2).setCellValue(record.getIntegratedApi() != null ? record.getIntegratedApi().getCode() : "");
                row.createCell(3).setCellValue(record.getIntegratedApi() != null ? record.getIntegratedApi().getName() : "");
                row.createCell(4).setCellValue(record.getMappingType() != null ? record.getMappingType().toString() : "");
                row.createCell(5).setCellValue(record.getData() != null ? record.getData() : "");
                row.createCell(6).setCellValue(record.getAttribute() != null ? record.getAttribute() : "");
                row.createCell(7).setCellValue(record.getExternalKey() != null ? record.getExternalKey() : "");
                row.createCell(8).setCellValue(record.getIsActive() ? "ACTIVE" : "INACTIVE");
                row.createCell(9).setCellValue(record.getNotes() != null ? record.getNotes() : "");
            }
            for (int i = 0; i <= 9; i++) {
                sheet.autoSizeColumn(i);
            }
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                workbook.write(bos);
                return bos.toByteArray();
            }
        }
    }

    @Transactional(readOnly = true)
    public List<IntegrationMappingDto> findAll() {
        return mappingRepository.findAll()
                .stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctMiddlewareApis() {
        return mappingRepository.findDistinctMiddlewareApiNames();
    }

    public Map<String, Object> validateMapping(IntegrationMappingRequestDto request) {
        Map<String, Object> validation = new HashMap<>();
        List<String> errors = new ArrayList<>();
        try {
            IntegrationMapping.MappingType type = IntegrationMapping.MappingType.valueOf(request.getMappingType());
            switch (type) {
                case DATA_ELEMENT_WITH_DISAGGREGATION:
                    if (!request.getData().contains(".")) {
                        errors.add("DATA_ELEMENT_WITH_DISAGGREGATION requires data in format DE_UID.COC_UID");
                    }
                    break;
                case DATA_ELEMENT:
                    if (request.getData().contains(".")) {
                        errors.add("DATA_ELEMENT should not contain disaggregation (no dots)");
                    }
                    break;
            }
        } catch (IllegalArgumentException e) {
            errors.add("Invalid mapping type: " + request.getMappingType());
        }
        if (!apiRepository.existsById(request.getIntegratedApiId())) {
            errors.add("Integrated API not found: " + request.getIntegratedApiId());
        }
        if (mappingRepository.existsByMiddlewareApiNameAndExternalKeyAndIsActiveTrue(
                request.getMiddlewareApiName(), request.getExternalKey())) {
            errors.add("External key already exists: " + request.getExternalKey());
        }
        validation.put("valid", errors.isEmpty());
        validation.put("errors", errors);
        return validation;
    }

    @Transactional
    public List<IntegrationMappingDto> createBatch(List<IntegrationMappingRequestDto> requests) {
        List<IntegrationMappingDto> created = new ArrayList<>();
        for (IntegrationMappingRequestDto request : requests) {
            try {
                created.add(create(request));
            } catch (Exception e) {
                log.error("Failed to create mapping: {}", e.getMessage());
                throw new RuntimeException("Batch creation failed at item " + created.size() + ": " + e.getMessage());
            }
        }
        return created;
    }

    @Transactional
    public Map<String, Object> importMappings(List<IntegrationMappingRequestDto> mappings) {
        Map<String, Object> result = new HashMap<>();
        List<IntegrationMappingDto> imported = new ArrayList<>();
        List<Map<String, Object>> errors = new ArrayList<>();
        for (int i = 0; i < mappings.size(); i++) {
            IntegrationMappingRequestDto request = mappings.get(i);
            try {
                imported.add(create(request));
            } catch (Exception e) {
                Map<String, Object> error = new HashMap<>();
                error.put("index", i);
                error.put("externalKey", request.getExternalKey());
                error.put("error", e.getMessage());
                errors.add(error);
            }
        }
        result.put("success", errors.isEmpty());
        result.put("imported", imported.size());
        result.put("errors", errors);
        return result;
    }

    private void mapRequestToEntity(IntegrationMappingRequestDto request, IntegrationMapping entity, IntegratedApi api) {
        entity.setMiddlewareApiName(request.getMiddlewareApiName());
        entity.setIntegratedApi(api);
        entity.setMappingType(IntegrationMapping.MappingType.valueOf(request.getMappingType()));
        entity.setData(request.getData());
        entity.setAttribute(request.getAttribute());
        entity.setExternalKey(request.getExternalKey());
        entity.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        entity.setNotes(request.getNotes());
    }

    private IntegrationMappingDto mapEntityToDto(IntegrationMapping entity) {
        return IntegrationMappingDto.builder()
                .id(entity.getId())
                .middlewareApiName(entity.getMiddlewareApiName())
                .integratedApiId(entity.getIntegratedApiId())
                .integratedApiCode(entity.getIntegratedApi() != null ? entity.getIntegratedApi().getCode() : null)
                .integratedApiName(entity.getIntegratedApi() != null ? entity.getIntegratedApi().getName() : null)
                .mappingType(entity.getMappingType().toString())
                .data(entity.getData())
                .attribute(entity.getAttribute())
                .externalKey(entity.getExternalKey())
                .isActive(entity.getIsActive())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
