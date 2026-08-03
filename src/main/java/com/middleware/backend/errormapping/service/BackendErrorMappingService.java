package com.middleware.backend.errormapping.service;

 import com.middleware.backend.dto.RouteOptionDto;
 import com.middleware.backend.errormapping.dto.ErrorMappingDto;
 import com.middleware.backend.errormapping.mapper.ErrorMappingMapper;
 import com.middleware.backend.errormapping.model.ErrorCategory;
 import com.middleware.backend.errormapping.model.ErrorMapping;
 import com.middleware.backend.errormapping.repository.ErrorCategoryRepository;
 import com.middleware.backend.errormapping.repository.ErrorMappingRepository;
 import com.middleware.backend.errormapping.spec.ErrorMappingSpecification;
 import com.middleware.backend.camel.repository.DynamicRouteRepository;

import com.middleware.backend.model.SourceSystem;

import com.middleware.backend.repository.SourceSystemRepository;
 import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
 import org.apache.poi.xssf.streaming.SXSSFWorkbook;
 import org.springframework.data.domain.Page;
 import org.springframework.data.domain.PageRequest;
 import org.springframework.data.domain.Pageable;
 import org.springframework.data.domain.Sort;
 import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
 import java.io.OutputStreamWriter;
 import java.io.PrintWriter;
 import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackendErrorMappingService {

    private final ErrorMappingRepository errorMappingRepository;
    private final DynamicRouteRepository dynamicRouteRepository;
    private final ErrorMappingMapper errorMappingMapper;
    private final ErrorCategoryRepository errorCategoryRepository;
    private final SourceSystemRepository sourceSystemRepository;

    // Get all error mappings with filters
    public Page<ErrorMappingDto> getAllErrorMappings(Map<String, String> filters, Pageable pageable) {
        Specification<ErrorMapping> spec = ErrorMappingSpecification.filter(filters);
        Page<ErrorMapping> entities = errorMappingRepository.findAll(spec, pageable);
        return entities.map(errorMappingMapper::toDto);
    }

    public Optional<ErrorMappingDto> getErrorMappingById(Long id) {
        return errorMappingRepository.findWithCategoryAndSourceSystemById(id)
                .map(errorMappingMapper::toDto);
    }

    public Optional<ErrorMappingDto> getErrorMappingByCode(String code) {
        return errorMappingRepository.findWithCategoryAndSourceSystemByCode(code)
                .map(errorMappingMapper::toDto);
    }

    private void validateErrorMappingDto(ErrorMappingDto dto) {
        if (dto.getRouteId() == null || dto.getRouteId().trim().isEmpty()) {
            dto.setRouteId("*");
        }
        if (dto.getRawErrorSubstring() == null || dto.getRawErrorSubstring().trim().isEmpty()) {
            throw new IllegalArgumentException("Raw error substring is required");
        }
        if (dto.getMappedErrorCode() == null || dto.getMappedErrorCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Mapped error code is required");
        }
        if (dto.getMappedMessage() == null || dto.getMappedMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("Mapped message is required");
        }
        if (dto.getHttpStatusCode() == null || dto.getHttpStatusCode() < 100 || dto.getHttpStatusCode() > 599) {
            throw new IllegalArgumentException("Valid HTTP status code is required (100-599)");
        }
        if (dto.getMatchType() == null) {
            throw new IllegalArgumentException("Match type is required");
        }

        if (dto.getMatchType() == ErrorMapping.MatchType.REGEX) {
            try {
                Pattern.compile(dto.getRawErrorSubstring());
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid regex pattern: " + e.getMessage());
            }
        }
    }
    public Optional<ErrorMappingDto> findMatchingErrorMapping(String routeId, Long sourceSystemId,
                                                              String errorMessage) {
        log.info("Database-level error matching - routeId: '{}', sourceSystemId: {}, errorMessage: '{}'", routeId,
                sourceSystemId, errorMessage);

        // Step 1: Find matching error mapping using native SQL with fallback logic
        Optional<ErrorMapping> basicResult = errorMappingRepository.findMatchingErrorWithFallback(routeId,
                sourceSystemId, errorMessage);

        if (basicResult.isPresent()) {
            // Step 2: Load the same entity with relations (SourceSystem, ErrorCategory)
            Optional<ErrorMapping> fullResult = errorMappingRepository.findByIdWithRelations(basicResult.get().getId());

            if (fullResult.isPresent()) {
                ErrorMapping mapping = fullResult.get();
                String sourceName = mapping.getSourceSystem() != null ? mapping.getSourceSystem().getName() : "GENERAL";
                String routeType = "*".equals(mapping.getRouteId()) ? "WILDCARD" : "SPECIFIC";

                log.info(
                        "MATCH FOUND - ErrorMapping ID: {}, Route: '{}' ({}), SourceSystem: '{}', MatchType: {}, Pattern: '{}'",
                        mapping.getId(), mapping.getRouteId(), routeType, sourceName, mapping.getMatchType(),
                        mapping.getRawErrorSubstring());

                return Optional.of(errorMappingMapper.toDto(mapping));
            }
        }

        log.info("NO MATCH FOUND for routeId: '{}', sourceSystemId: {}, errorMessage: '{}'", routeId, sourceSystemId,
                errorMessage);
        return Optional.empty();
    }
    @Transactional
    public ErrorMappingDto createErrorMapping(ErrorMappingDto dto) {
        validateErrorMappingDto(dto);

        boolean isWildcardRoute = "*".equals(dto.getRouteId());

        if (!isWildcardRoute && !routeExists(dto.getRouteId())) {
            throw new IllegalArgumentException("Route with ID '" + dto.getRouteId() + "' does not exist");
        }

        if (isWildcardRoute) {
            log.info(" Creating WILDCARD error mapping - applies to ALL routes and APIs");
        }

        if (errorMappingRepository.existsByRouteIdAndRawErrorSubstringAndActiveTrue(
                dto.getRouteId(), dto.getRawErrorSubstring())) {
            throw new IllegalArgumentException("Error mapping already exists for this route and error substring");
        }
        Optional<ErrorMapping> exists = errorMappingRepository.findByCode(dto.getCode());
        if(exists.isPresent())
            throw new IllegalArgumentException("Error mapping with Code: "+dto.getCode()+" already exists");
        dto.setCode(dto.getCode().trim().toUpperCase());
        ErrorMapping entity = errorMappingMapper.toEntity(dto);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUser = authentication.getName();

        entity.setCreatedBy(emailUser);
        entity.setUpdatedBy(emailUser);
        if (dto.getErrorCategoryId() != null) {
            ErrorCategory category = errorCategoryRepository.findById(dto.getErrorCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("ErrorCategory not found with ID: " + dto.getErrorCategoryId()));
            entity.setErrorCategory(category);
        }

        if (dto.getSourceSystemId() != null) {
            SourceSystem sourceSystem = sourceSystemRepository.findById(dto.getSourceSystemId())
                    .orElseThrow(() -> new EntityNotFoundException("SourceSystem not found with ID: ".concat(String.valueOf(dto.getSourceSystemId()))));
            entity.setSourceSystem(sourceSystem);
        }

        ErrorMapping saved = errorMappingRepository.save(entity);

        if (isWildcardRoute) {
            log.info("WILDCARD error mapping created successfully - ID: {}, Pattern: '{}', Code: '{}'",
                    saved.getId(), saved.getRawErrorSubstring(), saved.getMappedErrorCode());
        } else {
            log.info("Specific error mapping created - ID: {}, Route: '{}'", saved.getId(), saved.getRouteId());
        }

        return getErrorMappingById(saved.getId()).orElseThrow(() ->
                new IllegalStateException("Could not retrieve created error mapping with ID: " + saved.getId())
        );
    }

    @Transactional
    public ErrorMappingDto updateErrorMapping(Long id, ErrorMappingDto dto) {
        ErrorMapping existing = errorMappingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Error mapping not found with ID: " + id));

        validateErrorMappingDto(dto);

        if (!"*".equals(dto.getRouteId()) &&
                !existing.getRouteId().equals(dto.getRouteId()) &&
                !routeExists(dto.getRouteId())) {
            throw new IllegalArgumentException("Route with ID '" + dto.getRouteId() + "' does not exist");
        }

        // Update fields
        existing.setRouteId(dto.getRouteId());
        existing.setRoutePath(dto.getRoutePath());
        existing.setRawErrorSubstring(dto.getRawErrorSubstring());
        existing.setMatchType(dto.getMatchType());
        existing.setMappedErrorCode(dto.getMappedErrorCode());
        existing.setMappedMessage(dto.getMappedMessage());

        if (dto.getErrorCategoryId() != null) {
            ErrorCategory category = errorCategoryRepository.findById(dto.getErrorCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("ErrorCategory not found with ID: " + dto.getErrorCategoryId()));
            existing.setErrorCategory(category);
        } else {
            existing.setErrorCategory(null);
        }

        if (dto.getSourceSystemId() != null) {
            SourceSystem sourceSystem = sourceSystemRepository.findById(dto.getSourceSystemId())
                    .orElseThrow(() -> new EntityNotFoundException("SourceSystem not found with ID: " + dto.getSourceSystemId()));
            existing.setSourceSystem(sourceSystem);
        } else {
            existing.setSourceSystem(null);
        }

        existing.setHttpStatusCode(dto.getHttpStatusCode());
        existing.setLanguage(dto.getLanguage());
        existing.setActive(dto.getActive());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUser = authentication.getName();

        existing.setUpdatedBy(emailUser);
        existing.setUpdatedAt(LocalDateTime.now());

        ErrorMapping saved = errorMappingRepository.save(existing);

        String routeType = "*".equals(saved.getRouteId()) ? "WILDCARD" : "SPECIFIC";
        log.info("Updated error mapping with ID: {} for route: '{}' ({})", saved.getId(), saved.getRouteId(), routeType);

        // Since relations are loaded, we can map them directly.
        return getErrorMappingById(saved.getId()).orElseThrow(() ->
                new IllegalStateException("Could not retrieve updated error mapping with ID: " + saved.getId())
        );
    }


    public List<RouteOptionDto> getAvailableRoutes() {
        return dynamicRouteRepository.findLatestActiveRoutes()
                .stream()
                .map(route -> new RouteOptionDto(
                        route.getRouteId(),
                        route.getPath(),
                        route.getHttpMethod(),
                        route.getDescription()
                ))
                .collect(Collectors.toList());
    }


    @Transactional
    public void toggleErrorMapping(Long id) {
        ErrorMapping mapping = errorMappingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Error mapping not found with ID: " + id));

        mapping.setActive(!mapping.getActive());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUser = authentication.getName();

        mapping.setUpdatedBy(emailUser);
        mapping.setUpdatedAt(LocalDateTime.now());
        errorMappingRepository.save(mapping);

        log.info("Toggled error mapping {} to active: {}", id, mapping.getActive());
    }

    private boolean routeExists(String routeId) {
        return dynamicRouteRepository.findByRouteIdAndActiveTrue(routeId).stream()
                .findFirst()
                .isPresent();
    }



    public byte[] exportFile(Specification<ErrorMapping> spec, String type, String sortedBy, String sortDir) throws IOException {
        if ("CSV".equalsIgnoreCase(type)) {
            return convertToCSVStreamed(spec, sortedBy, sortDir);
        } else if ("Excel".equalsIgnoreCase(type) || "XLSX".equalsIgnoreCase(type)) {
            return convertToExcelStreamed(spec, sortedBy, sortDir);
        } else {
            throw new IllegalArgumentException("Unsupported export type: " + type);
        }
    }


    // === CSV Export ===
    private byte[] convertToCSVStreamed(Specification<ErrorMapping> spec, String sortedBy, String sortDir) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(bos, StandardCharsets.UTF_8))) {
            // Headers
            writer.println("Route ID,Route Path,Source System,Raw Error Substring,Match Type,Mapped Error Code,Mapped Message,Error Category,HTTP Status,Language,Active,Created At,Updated At");
            writer.flush();

            int pageSize = 1000;
            int pageNumber = 0;
            boolean hasMore = true;

            while (hasMore) {
                Sort sort = sortDir.equalsIgnoreCase("asc")
                        ? Sort.by(sortedBy).ascending()
                        : Sort.by(sortedBy).descending();
                Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
                Page<ErrorMapping> page = errorMappingRepository.findAll(spec, pageable);

                for (ErrorMapping em : page.getContent()) {
                    writer.append(escapeCsv(em.getRouteId())).append(",");
                    writer.append(escapeCsv(em.getRoutePath())).append(",");
                    writer.append(em.getSourceSystem() != null ? escapeCsv(em.getSourceSystem().getName()) : "").append(",");
                    writer.append(escapeCsv(em.getRawErrorSubstring())).append(",");
                    writer.append(em.getMatchType() != null ? escapeCsv(em.getMatchType().name()) : "").append(",");
                    writer.append(escapeCsv(em.getMappedErrorCode())).append(",");
                    writer.append(escapeCsv(em.getMappedMessage())).append(",");
                    writer.append(em.getErrorCategory() != null ? escapeCsv(em.getErrorCategory().getName()) : "").append(",");
                    writer.append(em.getHttpStatusCode() != null ? em.getHttpStatusCode().toString() : "").append(",");
                    writer.append(escapeCsv(em.getLanguage())).append(",");
                    writer.append(em.getActive() != null ? em.getActive().toString() : "").append(",");
                    writer.append(em.getCreatedAt() != null ? em.getCreatedAt().toString() : "").append(",");
                    writer.append(em.getUpdatedAt() != null ? em.getUpdatedAt().toString() : "").append("\n");
                }
                writer.flush();

                hasMore = page.hasNext();
                pageNumber++;
            }
            writer.flush();
        }

        return bos.toByteArray();
    }


    private String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private String safeString(String value) {
        return value != null ? value : "";
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength - 3) + "...";
    }


    // === Excel Export ===
    private byte[] convertToExcelStreamed(Specification<ErrorMapping> spec, String sortedBy, String sortDir) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        Sheet sheet = workbook.createSheet("Error Mappings");

        try {
            // Headers
            Row header = sheet.createRow(0);
            String[] columns = {
                    "Route ID", "Route Path", "Source System", "Raw Error Substring",
                    "Match Type", "Mapped Error Code", "Mapped Message", "Error Category",
                    "HTTP Status", "Language", "Active", "Created At", "Updated At"
            };
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            // Column widths
            int[] widths = {12, 25, 25, 40, 15, 25, 35, 25, 15, 15, 10, 25, 25};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            int rowIdx = 1;
            int pageSize = 1000;
            int pageNumber = 0;
            boolean hasMore = true;
            final int MAX_CELL_LENGTH = 20000;

            while (hasMore) {
                Sort sort = sortDir.equalsIgnoreCase("asc")
                        ? Sort.by(sortedBy).ascending()
                        : Sort.by(sortedBy).descending();
                Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
                Page<ErrorMapping> page = errorMappingRepository.findAll(spec, pageable);

                for (ErrorMapping em : page.getContent()) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(safeString(em.getRouteId()));
                    row.createCell(1).setCellValue(safeString(em.getRoutePath()));
                    row.createCell(2).setCellValue(em.getSourceSystem() != null ? em.getSourceSystem().getName() : "");
                    row.createCell(3).setCellValue(truncate(em.getRawErrorSubstring(), MAX_CELL_LENGTH));
                    row.createCell(4).setCellValue(em.getMatchType() != null ? em.getMatchType().name() : "");
                    row.createCell(5).setCellValue(safeString(em.getMappedErrorCode()));
                    row.createCell(6).setCellValue(truncate(em.getMappedMessage(), MAX_CELL_LENGTH));
                    row.createCell(7).setCellValue(em.getErrorCategory() != null ? em.getErrorCategory().getName() : "");
                    row.createCell(8).setCellValue(em.getHttpStatusCode() != null ? em.getHttpStatusCode() : 0);
                    row.createCell(9).setCellValue(safeString(em.getLanguage()));
                    row.createCell(10).setCellValue(em.getActive() != null ? em.getActive() : false);
                    row.createCell(11).setCellValue(em.getCreatedAt() != null ? em.getCreatedAt().toString() : "");
                    row.createCell(12).setCellValue(em.getUpdatedAt() != null ? em.getUpdatedAt().toString() : "");
                }

                hasMore = page.hasNext();
                pageNumber++;
            }

            workbook.write(bos);
            return bos.toByteArray();

        } finally {
            workbook.dispose();
        }
    }

}