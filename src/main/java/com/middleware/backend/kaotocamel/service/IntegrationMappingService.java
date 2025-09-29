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
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing IntegrationMapping entities
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntegrationMappingService {

    private final IntegrationMappingRepository mappingRepository;
    private final IntegratedApiRepository apiRepository;

    @Transactional
    public IntegrationMappingDto create(IntegrationMappingRequestDto request) {
        log.info("Creating integration mapping for API: {}", request.getMiddlewareApiName());

        // Validate integrated API exists
        IntegratedApi api = apiRepository.findById(request.getIntegratedApiId())
                .orElseThrow(() -> new RuntimeException("Integrated API not found: " + request.getIntegratedApiId()));

        // Check for duplicate external key
        if (mappingRepository.existsByMiddlewareApiNameAndExternalKeyAndIsActiveTrue(
                request.getMiddlewareApiName(), request.getExternalKey())) {
            throw new IllegalArgumentException("External key '" + request.getExternalKey() +
                    "' already exists for API: " + request.getMiddlewareApiName());
        }

        IntegrationMapping entity = new IntegrationMapping();
        mapRequestToEntity(request, entity, api);

        IntegrationMapping saved = mappingRepository.save(entity);
        log.info("Created integration mapping: {} -> {}",
                saved.getMiddlewareApiName(), saved.getExternalKey());

        return mapEntityToDto(saved);
    }

    @Transactional
    public IntegrationMappingDto update(Long id, IntegrationMappingRequestDto request) {
        log.info("Updating integration mapping: {}", id);

        IntegrationMapping entity = mappingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Integration mapping not found: " + id));

        // Validate integrated API if changed
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
    /**
     * Exports integration mappings to CSV or Excel bytes according to type.
     */
    public byte[] exportFile(Specification<IntegrationMapping> spec, Pageable pageable, String type) {
        List<IntegrationMapping> data;
        try {
            // استخدم query مخصصة مع JOIN FETCH
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

    /**
     * Helper method to fetch data with JOIN FETCH for export
     */
    private List<IntegrationMapping> findAllForExport(Specification<IntegrationMapping> spec, Pageable pageable) {
        return mappingRepository.findAll((root, query, criteriaBuilder) -> {
            // Apply JOIN FETCH
            root.fetch("integratedApi", JoinType.LEFT);
            query.distinct(true);

            // Apply the original specification
            return spec != null ? spec.toPredicate(root, query, criteriaBuilder) : null;
        }, pageable).getContent();
    }

    // ================= CSV Export =================
    private String convertToCSV(List<IntegrationMapping> records) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Middleware API Name,Integrated API ID,Integrated API Code,Integrated API Name,");
        sb.append("Mapping Type,Data,Attribute,External Key,Active,Notes,Created At,Updated At\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (IntegrationMapping record : records) {
            sb.append(record.getId()).append(",");
            sb.append(escapeCsv(record.getMiddlewareApiName())).append(",");
            sb.append(record.getIntegratedApiId()).append(",");
            sb.append(escapeCsv(record.getIntegratedApi() != null ? record.getIntegratedApi().getCode() : "")).append(",");
            sb.append(escapeCsv(record.getIntegratedApi() != null ? record.getIntegratedApi().getName() : "")).append(",");
            sb.append(record.getMappingType() != null ? record.getMappingType().toString() : "").append(",");
            sb.append(escapeCsv(record.getData())).append(",");
            sb.append(escapeCsv(record.getAttribute())).append(",");
            sb.append(escapeCsv(record.getExternalKey())).append(",");
            sb.append(record.getIsActive() ? "ACTIVE" : "INACTIVE").append(",");
            sb.append(escapeCsv(record.getNotes())).append(",");
            sb.append(record.getCreatedAt() != null ? record.getCreatedAt().format(formatter) : "").append(",");
            sb.append(record.getUpdatedAt() != null ? record.getUpdatedAt().format(formatter) : "").append("\n");
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

    // ================= Excel Export =================
    private byte[] convertToExcel(List<IntegrationMapping> records) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Integration Mappings");

            // Header row
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("Middleware API Name");
            header.createCell(2).setCellValue("Integrated API ID");
            header.createCell(3).setCellValue("Integrated API Code");
            header.createCell(4).setCellValue("Integrated API Name");
            header.createCell(5).setCellValue("Mapping Type");
            header.createCell(6).setCellValue("Data");
            header.createCell(7).setCellValue("Attribute");
            header.createCell(8).setCellValue("External Key");
            header.createCell(9).setCellValue("Active");
            header.createCell(10).setCellValue("Notes");
            header.createCell(11).setCellValue("Created At");
            header.createCell(12).setCellValue("Updated At");

            // Data rows
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            int rowIdx = 1;
            for (IntegrationMapping record : records) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(record.getId() != null ? record.getId() : 0);
                row.createCell(1).setCellValue(record.getMiddlewareApiName() != null ? record.getMiddlewareApiName() : "");
                row.createCell(2).setCellValue(record.getIntegratedApiId() != null ? record.getIntegratedApiId() : 0);
                row.createCell(3).setCellValue(record.getIntegratedApi() != null ? record.getIntegratedApi().getCode() : "");
                row.createCell(4).setCellValue(record.getIntegratedApi() != null ? record.getIntegratedApi().getName() : "");
                row.createCell(5).setCellValue(record.getMappingType() != null ? record.getMappingType().toString() : "");
                row.createCell(6).setCellValue(record.getData() != null ? record.getData() : "");
                row.createCell(7).setCellValue(record.getAttribute() != null ? record.getAttribute() : "");
                row.createCell(8).setCellValue(record.getExternalKey() != null ? record.getExternalKey() : "");
                row.createCell(9).setCellValue(record.getIsActive() ? "ACTIVE" : "INACTIVE");
                row.createCell(10).setCellValue(record.getNotes() != null ? record.getNotes() : "");
                row.createCell(11).setCellValue(record.getCreatedAt() != null ? record.getCreatedAt().format(formatter) : "");
                row.createCell(12).setCellValue(record.getUpdatedAt() != null ? record.getUpdatedAt().format(formatter) : "");
            }

            // Auto-size columns
            for (int i = 0; i <= 12; i++) {
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

        // Validate mapping type and data format
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

        // Check if API exists
        if (!apiRepository.existsById(request.getIntegratedApiId())) {
            errors.add("Integrated API not found: " + request.getIntegratedApiId());
        }

        // Check for duplicate external key
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