//package com.middleware.backend.kaotocamel.service;
//
//import com.middleware.backend.kaotocamel.dto.AnalyticsResponseDto;
//import com.middleware.backend.kaotocamel.dto.IntegratedApiDto;
//import com.middleware.backend.kaotocamel.dto.IntegratedApiRequestDto;
//import com.middleware.backend.kaotocamel.model.IntegratedApi;
//import com.middleware.backend.kaotocamel.repository.IntegratedApiRepository;
//import com.middleware.backend.kaotocamel.spec.IntegratedApiSpecification;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.poi.ss.usermodel.Row;
//import org.apache.poi.ss.usermodel.Sheet;
//import org.apache.poi.ss.usermodel.Workbook;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.domain.Specification;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.*;
//import java.util.stream.Collectors;
//
///**
// * Enhanced Service for managing IntegratedApi entities with dynamic DHIS2 code support
// */
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class IntegratedApiService {
//
//    private final IntegratedApiRepository repository;
//    private final Dhis2ClientService dhis2Client;
//
//    @Transactional
//    public IntegratedApiDto create(IntegratedApiRequestDto request) {
//        log.info("Creating integrated API with code: {}", request.getCode());
//
//        if (repository.existsByCode(request.getCode())) {
//            throw new IllegalArgumentException("API with code '" + request.getCode() + "' already exists");
//        }
//
//        IntegratedApi entity = new IntegratedApi();
//        mapRequestToEntity(request, entity);
//
//        IntegratedApi saved = repository.save(entity);
//        log.info("Created integrated API: {} (ID: {})", saved.getCode(), saved.getId());
//
//        return mapEntityToDto(saved);
//    }
//
//    @Transactional
//    public IntegratedApiDto update(Long id, IntegratedApiRequestDto request) {
//        log.info("Updating integrated API: {}", id);
//
//        IntegratedApi entity = repository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Integrated API not found: " + id));
//
//        if (!entity.getCode().equals(request.getCode()) &&
//                repository.existsByCode(request.getCode())) {
//            throw new IllegalArgumentException("API with code '" + request.getCode() + "' already exists");
//        }
//
//        mapRequestToEntity(request, entity);
//        IntegratedApi saved = repository.save(entity);
//
//        log.info("Updated integrated API: {}", saved.getCode());
//        return mapEntityToDto(saved);
//    }
//
//    @Transactional
//    public void softDelete(Long id) {
//        log.info("Soft deleting integrated API: {}", id);
//
//        IntegratedApi entity = repository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Integrated API not found: " + id));
//
//        entity.setIsActive(false);
//        repository.save(entity);
//
//        log.info("Soft deleted integrated API: {}", entity.getCode());
//    }
//
//    @Transactional(readOnly = true)
//    public Optional<IntegratedApiDto> findById(Long id) {
//        return repository.findById(id)
//                .map(this::mapEntityToDto);
//    }
//
//    @Transactional(readOnly = true)
//    public Optional<IntegratedApiDto> findByCode(String code) {
//        return repository.findByCode(code)
//                .map(this::mapEntityToDto);
//    }
//
//    @Transactional(readOnly = true)
//    public Page<IntegratedApiDto> findWithFilters(String code, String name, String type,
//                                                  String integratedSystem, Boolean isActive,
//                                                  Pageable pageable) {
//        return findWithAdvancedFilters(code, name, null, type, integratedSystem, isActive,
//                null, null, null, null, null, null, null, null, pageable);
//    }
//
//    @Transactional(readOnly = true)
//    public Page<IntegratedApiDto> findWithAdvancedFilters(
//            String code, String name, String apiUrl, String type,
//            String integratedSystem, Boolean isActive, String description,
//            LocalDateTime createdAfter, LocalDateTime createdBefore,
//            LocalDateTime updatedAfter, LocalDateTime updatedBefore,
//            String search, Long minId, Long maxId,
//            Pageable pageable) {
//
//        Specification<IntegratedApi> spec = IntegratedApiSpecification.buildSpecification(
//                code, name, apiUrl, type, integratedSystem, isActive, description,
//                createdAfter, createdBefore, updatedAfter, updatedBefore,
//                search, minId, maxId
//        );
//
//        return repository.findAll(spec, pageable)
//                .map(this::mapEntityToDto);
//    }
//
//    @Transactional(readOnly = true)
//    public List<String> getDistinctIntegratedSystems() {
//        return repository.findAll().stream()
//                .map(IntegratedApi::getIntegratedSystem)
//                .distinct()
//                .sorted()
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Dynamic DHIS2 connection test supporting _dhis2Code
//     */
//    public Map<String, Object> testApiConnection(Long id, String period, String dhis2Code) {
//        log.info("Testing API connection for ID: {} with period: {} and DHIS2 code: {}", id, period, dhis2Code);
//
//        IntegratedApi api = repository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Integrated API not found: " + id));
//
//        Map<String, Object> result = new HashMap<>();
//
//        try {
//            Map<String, Object> response = dhis2Client.executeApiCall(api, period, dhis2Code);
//
//            result.put("success", true);
//            result.put("message", "Connection successful");
//            result.put("responseType", response.get("type"));
//
//            if ("ANALYTICS".equals(response.get("type"))) {
//                AnalyticsResponseDto analytics = (AnalyticsResponseDto) response.get("data");
//                result.put("rowCount", analytics.getRows().size());
//            }
//
//        } catch (Exception e) {
//            result.put("success", false);
//            result.put("message", "Connection failed");
//            result.put("error", e.getMessage());
//        }
//
//        return result;
//    }
//
//    private void mapRequestToEntity(IntegratedApiRequestDto request, IntegratedApi entity) {
//        entity.setCode(request.getCode());
//        entity.setName(request.getName());
//        entity.setApiUrl(request.getApiUrl());
//        entity.setType(IntegratedApi.ApiType.valueOf(request.getType()));
//        entity.setIntegratedSystem(request.getIntegratedSystem());
//        entity.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
//        entity.setDescription(request.getDescription());
//    }
//    /**
//     * Exports integrated APIs to CSV or Excel bytes according to type.
//     */
//    public byte[] exportFile(Specification<IntegratedApi> spec, Pageable pageable, String type) {
//        List<IntegratedApi> data;
//        try {
//            Page<IntegratedApi> res = repository.findAll(spec, pageable);
//            data = res.getContent();
//        } catch (Exception e) {
//            log.error("Error fetching data for export", e);
//            data = Collections.emptyList();
//        }
//
//        try {
//            if ("CSV".equalsIgnoreCase(type)) {
//                return convertToCSV(data).getBytes(StandardCharsets.UTF_8);
//            } else if ("Excel".equalsIgnoreCase(type) || "XLSX".equalsIgnoreCase(type)) {
//                return convertToExcel(data);
//            } else {
//                throw new IllegalArgumentException("Unsupported export type: " + type);
//            }
//        } catch (IOException e) {
//            throw new RuntimeException("Failed to export file", e);
//        }
//    }
//
//    // ================= CSV Export =================
//    private String convertToCSV(List<IntegratedApi> records) {
//        StringBuilder sb = new StringBuilder();
//        sb.append("ID,Code,Name,API URL,Type,Integrated System,Active,Description,Created At,Updated At\n");
//
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//
//        for (IntegratedApi record : records) {
//            sb.append(record.getId()).append(",");
//            sb.append(escapeCsv(record.getCode())).append(",");
//            sb.append(escapeCsv(record.getName())).append(",");
//            sb.append(escapeCsv(record.getApiUrl())).append(",");
//            sb.append(record.getType() != null ? record.getType().toString() : "").append(",");
//            sb.append(escapeCsv(record.getIntegratedSystem())).append(",");
//            sb.append(record.getIsActive() ? "ACTIVE" : "INACTIVE").append(",");
//            sb.append(escapeCsv(record.getDescription())).append(",");
//            sb.append(record.getCreatedAt() != null ? record.getCreatedAt().format(formatter) : "").append(",");
//            sb.append(record.getUpdatedAt() != null ? record.getUpdatedAt().format(formatter) : "").append("\n");
//        }
//
//        return sb.toString();
//    }
//
//    private String escapeCsv(String value) {
//        if (value == null) return "";
//        String escaped = value.replace("\"", "\"\"");
//        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
//            return "\"" + escaped + "\"";
//        }
//        return escaped;
//    }
//
//    // ================= Excel Export =================
//    private byte[] convertToExcel(List<IntegratedApi> records) throws IOException {
//        try (Workbook workbook = new XSSFWorkbook()) {
//            Sheet sheet = workbook.createSheet("Integrated APIs");
//
//            // Header row
//            Row header = sheet.createRow(0);
//            header.createCell(0).setCellValue("ID");
//            header.createCell(1).setCellValue("Code");
//            header.createCell(2).setCellValue("Name");
//            header.createCell(3).setCellValue("API URL");
//            header.createCell(4).setCellValue("Type");
//            header.createCell(5).setCellValue("Integrated System");
//            header.createCell(6).setCellValue("Active");
//            header.createCell(7).setCellValue("Description");
//            header.createCell(8).setCellValue("Created At");
//            header.createCell(9).setCellValue("Updated At");
//
//            // Data rows
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//            int rowIdx = 1;
//            for (IntegratedApi record : records) {
//                Row row = sheet.createRow(rowIdx++);
//                row.createCell(0).setCellValue(record.getId() != null ? record.getId() : 0);
//                row.createCell(1).setCellValue(record.getCode() != null ? record.getCode() : "");
//                row.createCell(2).setCellValue(record.getName() != null ? record.getName() : "");
//                row.createCell(3).setCellValue(record.getApiUrl() != null ? record.getApiUrl() : "");
//                row.createCell(4).setCellValue(record.getType() != null ? record.getType().toString() : "");
//                row.createCell(5).setCellValue(record.getIntegratedSystem() != null ? record.getIntegratedSystem() : "");
//                row.createCell(6).setCellValue(record.getIsActive() ? "ACTIVE" : "INACTIVE");
//                row.createCell(7).setCellValue(record.getDescription() != null ? record.getDescription() : "");
//                row.createCell(8).setCellValue(record.getCreatedAt() != null ? record.getCreatedAt().format(formatter) : "");
//                row.createCell(9).setCellValue(record.getUpdatedAt() != null ? record.getUpdatedAt().format(formatter) : "");
//            }
//
//            // Auto-size columns
//            for (int i = 0; i <= 9; i++) {
//                sheet.autoSizeColumn(i);
//            }
//
//            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
//                workbook.write(bos);
//                return bos.toByteArray();
//            }
//        }
//    }
//    private IntegratedApiDto mapEntityToDto(IntegratedApi entity) {
//        return IntegratedApiDto.builder()
//                .id(entity.getId())
//                .code(entity.getCode())
//                .name(entity.getName())
//                .apiUrl(entity.getApiUrl())
//                .type(entity.getType().toString())
//                .integratedSystem(entity.getIntegratedSystem())
//                .isActive(entity.getIsActive())
//                .description(entity.getDescription())
//                .createdAt(entity.getCreatedAt())
//                .updatedAt(entity.getUpdatedAt())
//                .build();
//    }
//}
