//package com.middleware.backend.errormapping.service;
//
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//import java.util.regex.Pattern;
//import java.util.stream.Collectors;
//
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
//import com.middleware.backend.dto.RouteOptionDto;
//import com.middleware.backend.errormapping.dto.ErrorMappingDto;
//import com.middleware.backend.errormapping.mapper.ErrorMappingMapper;
//import com.middleware.backend.errormapping.model.ErrorCategory;
//import com.middleware.backend.errormapping.model.ErrorMapping;
//import com.middleware.backend.errormapping.repository.ErrorCategoryRepository;
//import com.middleware.backend.errormapping.repository.ErrorMappingRepository;
//import com.middleware.backend.errormapping.spec.ErrorMappingSpecification;
//import com.middleware.backend.kaotocamel.repository.DynamicRouteRepository;
//import com.middleware.backend.model.SourceSystem;
//import com.middleware.backend.repository.SourceSystemRepository;
//
//import jakarta.persistence.EntityNotFoundException;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class BackendErrorMappingService {
//
//    private final ErrorMappingRepository errorMappingRepository;
//    private final DynamicRouteRepository dynamicRouteRepository;
//    private final ErrorMappingMapper errorMappingMapper;
//    private final ErrorCategoryRepository errorCategoryRepository;
//    private final SourceSystemRepository sourceSystemRepository;
//
//    // Get all error mappings with filters
//    public Page<ErrorMappingDto> getAllErrorMappings(Map<String, String> filters, Pageable pageable) {
//        Specification<ErrorMapping> spec = ErrorMappingSpecification.filter(filters);
//        Page<ErrorMapping> entities = errorMappingRepository.findAll(spec, pageable);
//        return entities.map(errorMappingMapper::toDto);
//    }
//
//    public Optional<ErrorMappingDto> getErrorMappingById(Long id) {
//        return errorMappingRepository.findWithCategoryAndSourceSystemById(id).map(errorMappingMapper::toDto);
//    }
//
//    private void validateErrorMappingDto(ErrorMappingDto dto) {
//        if (dto.getRouteId() == null || dto.getRouteId().trim().isEmpty()) {
//            dto.setRouteId("*");
//        }
//        if (dto.getRawErrorSubstring() == null || dto.getRawErrorSubstring().trim().isEmpty()) {
//            throw new IllegalArgumentException("Raw error substring is required");
//        }
//        if (dto.getMappedErrorCode() == null || dto.getMappedErrorCode().trim().isEmpty()) {
//            throw new IllegalArgumentException("Mapped error code is required");
//        }
//        if (dto.getMappedMessage() == null || dto.getMappedMessage().trim().isEmpty()) {
//            throw new IllegalArgumentException("Mapped message is required");
//        }
//        if (dto.getHttpStatusCode() == null || dto.getHttpStatusCode() < 100 || dto.getHttpStatusCode() > 599) {
//            throw new IllegalArgumentException("Valid HTTP status code is required (100-599)");
//        }
//        if (dto.getMatchType() == null) {
//            throw new IllegalArgumentException("Match type is required");
//        }
//
//        if (dto.getMatchType() == ErrorMapping.MatchType.REGEX) {
//            try {
//                Pattern.compile(dto.getRawErrorSubstring());
//            } catch (Exception e) {
//                throw new IllegalArgumentException("Invalid regex pattern: " + e.getMessage());
//            }
//        }
//    }
//
//    @Transactional
//    public ErrorMappingDto createErrorMapping(ErrorMappingDto dto) {
//        validateErrorMappingDto(dto);
//
//        boolean isWildcardRoute = "*".equals(dto.getRouteId());
//
//        if (!isWildcardRoute && !routeExists(dto.getRouteId())) {
//            throw new IllegalArgumentException("Route with ID '" + dto.getRouteId() + "' does not exist");
//        }
//
//        if (isWildcardRoute) {
//            log.info(" Creating WILDCARD error mapping - applies to ALL routes and APIs");
//        }
//
//        if (errorMappingRepository.existsByRouteIdAndRawErrorSubstringAndActiveTrue(dto.getRouteId(),
//                dto.getRawErrorSubstring())) {
//            throw new IllegalArgumentException("Error mapping already exists for this route and error substring");
//        }
//
//        ErrorMapping entity = errorMappingMapper.toEntity(dto);
//
//        Long currentUserId = 1L;
//        entity.setCreatedBy(String.valueOf(currentUserId));
//        if (dto.getErrorCategoryId() != null) {
//            ErrorCategory category = errorCategoryRepository.findById(dto.getErrorCategoryId()).orElseThrow(
//                    () -> new EntityNotFoundException("ErrorCategory not found with ID: " + dto.getErrorCategoryId()));
//            entity.setErrorCategory(category);
//        }
//
//        if (dto.getSourceSystemId() != null) {
//            SourceSystem sourceSystem = sourceSystemRepository.findById(dto.getSourceSystemId())
//                    .orElseThrow(() -> new EntityNotFoundException(
//                            "SourceSystem not found with ID: ".concat(String.valueOf(dto.getSourceSystemId()))));
//            entity.setSourceSystem(sourceSystem);
//        }
//
//        ErrorMapping saved = errorMappingRepository.save(entity);
//
//        if (isWildcardRoute) {
//            log.info("WILDCARD error mapping created successfully - ID: {}, Pattern: '{}', Code: '{}'", saved.getId(),
//                    saved.getRawErrorSubstring(), saved.getMappedErrorCode());
//        } else {
//            log.info("Specific error mapping created - ID: {}, Route: '{}'", saved.getId(), saved.getRouteId());
//        }
//
//        return getErrorMappingById(saved.getId()).orElseThrow(
//                () -> new IllegalStateException("Could not retrieve created error mapping with ID: " + saved.getId()));
//    }
//
//    @Transactional
//    public ErrorMappingDto updateErrorMapping(Long id, ErrorMappingDto dto) {
//        ErrorMapping existing = errorMappingRepository.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("Error mapping not found with ID: " + id));
//
//        validateErrorMappingDto(dto);
//
//        if (!"*".equals(dto.getRouteId()) && !existing.getRouteId().equals(dto.getRouteId())
//                && !routeExists(dto.getRouteId())) {
//            throw new IllegalArgumentException("Route with ID '" + dto.getRouteId() + "' does not exist");
//        }
//
//        // Update fields
//        existing.setRouteId(dto.getRouteId());
//        existing.setRoutePath(dto.getRoutePath());
//        existing.setRawErrorSubstring(dto.getRawErrorSubstring());
//        existing.setMatchType(dto.getMatchType());
//        existing.setMappedErrorCode(dto.getMappedErrorCode());
//        existing.setMappedMessage(dto.getMappedMessage());
//
//        if (dto.getErrorCategoryId() != null) {
//            ErrorCategory category = errorCategoryRepository.findById(dto.getErrorCategoryId()).orElseThrow(
//                    () -> new EntityNotFoundException("ErrorCategory not found with ID: " + dto.getErrorCategoryId()));
//            existing.setErrorCategory(category);
//        } else {
//            existing.setErrorCategory(null);
//        }
//
//        if (dto.getSourceSystemId() != null) {
//            SourceSystem sourceSystem = sourceSystemRepository.findById(dto.getSourceSystemId()).orElseThrow(
//                    () -> new EntityNotFoundException("SourceSystem not found with ID: " + dto.getSourceSystemId()));
//            existing.setSourceSystem(sourceSystem);
//        } else {
//            existing.setSourceSystem(null);
//        }
//
//        existing.setHttpStatusCode(dto.getHttpStatusCode());
//        existing.setLanguage(dto.getLanguage());
//        existing.setActive(dto.getActive());
//
//        Long currentUserId = 1L;
//        existing.setUpdatedBy(String.valueOf(currentUserId));
//
//        ErrorMapping saved = errorMappingRepository.save(existing);
//
//        String routeType = "*".equals(saved.getRouteId()) ? "WILDCARD" : "SPECIFIC";
//        log.info("Updated error mapping with ID: {} for route: '{}' ({})", saved.getId(), saved.getRouteId(),
//                routeType);
//
//        // Since relations are loaded, we can map them directly.
//        return getErrorMappingById(saved.getId()).orElseThrow(
//                () -> new IllegalStateException("Could not retrieve updated error mapping with ID: " + saved.getId()));
//    }
//
//    @Transactional
//    public void deleteErrorMapping(Long id) {
//        if (!errorMappingRepository.existsById(id)) {
//            throw new EntityNotFoundException("Error mapping not found with ID: " + id);
//        }
//        errorMappingRepository.deleteById(id);
//        log.info("Deleted error mapping with ID: {}", id);
//    }
//
//    public List<RouteOptionDto> getAvailableRoutes() {
//        return dynamicRouteRepository.findLatestActiveRoutes().stream()
//                .map(route -> new RouteOptionDto(route.getRouteId(), route.getPath(), route.getHttpMethod(),
//                        route.getDescription()))
//                .collect(Collectors.toList());
//    }
//
//    public long getCountByRouteId(String routeId) {
//        return errorMappingRepository.countActiveByRouteId(routeId);
//    }
//
//    @Transactional
//    public void toggleErrorMapping(Long id) {
//        ErrorMapping mapping = errorMappingRepository.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("Error mapping not found with ID: " + id));
//
//        mapping.setActive(!mapping.getActive());
//        errorMappingRepository.save(mapping);
//
//        log.info("Toggled error mapping {} to active: {}", id, mapping.getActive());
//    }
//
//    private boolean routeExists(String routeId) {
//        return dynamicRouteRepository.findByRouteIdAndActiveTrue(routeId).stream().findFirst().isPresent();
//    }
//
//    public Optional<ErrorMappingDto> findMatchingErrorMapping(String routeId, Long sourceSystemId,
//            String errorMessage) {
//        log.info("Database-level error matching - routeId: '{}', sourceSystemId: {}, errorMessage: '{}'", routeId,
//                sourceSystemId, errorMessage);
//
//        // Step 1: Find matching error mapping using native SQL with fallback logic
//        Optional<ErrorMapping> basicResult = errorMappingRepository.findMatchingErrorWithFallback(routeId,
//                sourceSystemId, errorMessage);
//
//        if (basicResult.isPresent()) {
//            // Step 2: Load the same entity with relations (SourceSystem, ErrorCategory)
//            Optional<ErrorMapping> fullResult = errorMappingRepository.findByIdWithRelations(basicResult.get().getId());
//
//            if (fullResult.isPresent()) {
//                ErrorMapping mapping = fullResult.get();
//                String sourceName = mapping.getSourceSystem() != null ? mapping.getSourceSystem().getName() : "GENERAL";
//                String routeType = "*".equals(mapping.getRouteId()) ? "WILDCARD" : "SPECIFIC";
//
//                log.info(
//                        "MATCH FOUND - ErrorMapping ID: {}, Route: '{}' ({}), SourceSystem: '{}', MatchType: {}, Pattern: '{}'",
//                        mapping.getId(), mapping.getRouteId(), routeType, sourceName, mapping.getMatchType(),
//                        mapping.getRawErrorSubstring());
//
//                return Optional.of(errorMappingMapper.toDto(mapping));
//            }
//        }
//
//        log.info("NO MATCH FOUND for routeId: '{}', sourceSystemId: {}, errorMessage: '{}'", routeId, sourceSystemId,
//                errorMessage);
//        return Optional.empty();
//    }

//    public byte[] exportFile(Map<String, String> filters, Pageable pageable, String type) {
//        // Build dynamic specification from filters
//        Specification<ErrorMapping> spec = ErrorMappingSpecification.filter(filters);
//
//        // Fetch filtered data
//        Page<ErrorMapping> page = errorMappingRepository.findAll(spec, pageable);
//        List<ErrorMapping> data = page.getContent();
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
//            throw new RuntimeException("Error generating export file", e);
//        }
//    }
//
//    // === CSV Export ===
//    private String convertToCSV(List<ErrorMapping> mappings) {
//        StringBuilder sb = new StringBuilder();
//        sb.append(
//                "Route ID,Route Path,Source System,Raw Error Substring,Match Type,Mapped Error Code,Mapped Message,Error Category,HTTP Status,Language,Active,Created At,Updated At\n");
//
//        for (ErrorMapping em : mappings) {
//            sb.append(escapeCsv(em.getRouteId())).append(",");
//            sb.append(escapeCsv(em.getRoutePath())).append(",");
//            sb.append(em.getSourceSystem() != null ? escapeCsv(em.getSourceSystem().getName()) : "").append(",");
//            sb.append(escapeCsv(em.getRawErrorSubstring())).append(",");
//            sb.append(em.getMatchType() != null ? escapeCsv(em.getMatchType().name()) : "").append(",");
//            sb.append(escapeCsv(em.getMappedErrorCode())).append(",");
//            sb.append(escapeCsv(em.getMappedMessage())).append(",");
//            sb.append(em.getErrorCategory() != null ? escapeCsv(em.getErrorCategory().getName()) : "").append(",");
//            sb.append(em.getHttpStatusCode() != null ? em.getHttpStatusCode() : "").append(",");
//            sb.append(escapeCsv(em.getLanguage())).append(",");
//            sb.append(em.getActive() != null ? em.getActive() : "").append(",");
//            sb.append(em.getCreatedAt() != null ? em.getCreatedAt() : "").append(",");
//            sb.append(em.getUpdatedAt() != null ? em.getUpdatedAt() : "").append("\n");
//        }
//        return sb.toString();
//    }
//
//    private String escapeCsv(String value) {
//        if (value == null)
//            return "";
//        String escaped = value.replace("\"", "\"\"");
//        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
//            return "\"" + escaped + "\"";
//        }
//        return escaped;
//    }
//
//    // === Excel Export ===
//    private byte[] convertToExcel(List<ErrorMapping> mappings) throws IOException {
//        try (Workbook workbook = new XSSFWorkbook()) {
//            Sheet sheet = workbook.createSheet("Error Mappings");
//
//            // Header row
//            Row header = sheet.createRow(0);
//            String[] headers = { "Route ID", "Route Path", "Source System", "Raw Error Substring", "Match Type",
//                    "Mapped Error Code", "Mapped Message", "Error Category", "HTTP Status", "Language", "Active",
//                    "Created At", "Updated At" };
//
//            for (int i = 0; i < headers.length; i++) {
//                header.createCell(i).setCellValue(headers[i]);
//            }
//
//            // Data rows
//            int rowIdx = 1;
//            for (ErrorMapping em : mappings) {
//                Row row = sheet.createRow(rowIdx++);
//                row.createCell(0).setCellValue(em.getRouteId());
//                row.createCell(1).setCellValue(em.getRoutePath() != null ? em.getRoutePath() : "");
//                row.createCell(2).setCellValue(em.getSourceSystem() != null ? em.getSourceSystem().getName() : "");
//                row.createCell(3).setCellValue(em.getRawErrorSubstring() != null ? em.getRawErrorSubstring() : "");
//                row.createCell(4).setCellValue(em.getMatchType() != null ? em.getMatchType().name() : "");
//                row.createCell(5).setCellValue(em.getMappedErrorCode() != null ? em.getMappedErrorCode() : "");
//                row.createCell(6).setCellValue(em.getMappedMessage() != null ? em.getMappedMessage() : "");
//                row.createCell(7).setCellValue(em.getErrorCategory() != null ? em.getErrorCategory().getName() : "");
//                row.createCell(8).setCellValue(em.getHttpStatusCode() != null ? em.getHttpStatusCode() : 0);
//                row.createCell(9).setCellValue(em.getLanguage() != null ? em.getLanguage() : "");
//                row.createCell(10).setCellValue(em.getActive() != null ? em.getActive() : false);
//                row.createCell(11).setCellValue(em.getCreatedAt() != null ? em.getCreatedAt().toString() : "");
//                row.createCell(12).setCellValue(em.getUpdatedAt() != null ? em.getUpdatedAt().toString() : "");
//            }
//
//            // Auto-size columns
//            for (int i = 0; i < headers.length; i++) {
//                sheet.autoSizeColumn(i);
//            }
//
//            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
//                workbook.write(bos);
//                return bos.toByteArray();
//            }
//        }
//    }
//}