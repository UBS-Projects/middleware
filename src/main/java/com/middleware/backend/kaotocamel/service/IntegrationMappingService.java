package com.middleware.backend.kaotocamel.service;

import com.middleware.backend.kaotocamel.dto.*;
import com.middleware.backend.kaotocamel.model.*;
import com.middleware.backend.kaotocamel.repository.*;
import com.middleware.backend.kaotocamel.spec.IntegrationMappingSpecification;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntegrationMappingService {

    private final IntegrationMappingRepository mappingRepository;
    private final IntegratedApiRepository apiRepository;
    private final DynamicRouteRepository dynamicRouteRepository;

    @Transactional
    public IntegrationMappingDto create(IntegrationMappingRequestDto request) {
        log.info("Creating integration mapping for Dynamic Route: {}", request.getDynamicRouteId());

        validateDynamicRouteExists(request.getDynamicRouteId());

        IntegratedApi api = apiRepository.findById(request.getIntegratedApiId())
                .orElseThrow(() -> new RuntimeException("Integrated API not found: " + request.getIntegratedApiId()));

         IntegrationMapping.MappingType mappingType = IntegrationMapping.MappingType.valueOf(request.getMappingType());

        boolean isDuplicate = mappingRepository.existsByDynamicRouteIdAndIntegratedApiIdAndMappingTypeAndDataAndExternalKey(
                request.getDynamicRouteId(),
                request.getIntegratedApiId(),
                mappingType,
                request.getData(),
                request.getExternalKey()
        );

        if (isDuplicate) {
            String errorMsg = String.format(
                    "Duplicate Mapping Detected!\n\n" +
                            "A mapping with this exact combination already exists:\n\n" +
                            "• Dynamic Route ID: %s\n" +
                            "• Integrated API ID: %d\n" +
                            "• Mapping Type: %s\n" +
                            "• Data: %s\n" +
                            "• External Key: %s\n\n" +
                            "Please modify one or more of these values to create a unique mapping.",
                    request.getDynamicRouteId(),
                    request.getIntegratedApiId(),
                    request.getMappingType(),
                    request.getData(),
                    request.getExternalKey()
            );

            log.warn("Duplicate mapping attempt: {}", errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        IntegrationMapping entity = new IntegrationMapping();
        mapRequestToEntity(request, entity, api);

        entity.setCreatedBy(currentUser);
        entity.setUpdatedBy(currentUser);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        IntegrationMapping saved = mappingRepository.save(entity);
        log.info("Created integration mapping: {} -> {} by user: {}",
                saved.getDynamicRouteId(), saved.getExternalKey(), currentUser);

        return mapEntityToDto(saved);
    }

    @Transactional
    public IntegrationMappingDto update(Long id, IntegrationMappingRequestDto request) {
        log.info("Updating integration mapping: {}", id);

        IntegrationMapping entity = mappingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Integration mapping not found: " + id));

        validateDynamicRouteExists(request.getDynamicRouteId());

        IntegratedApi api = entity.getIntegratedApi();
        if (!entity.getIntegratedApiId().equals(request.getIntegratedApiId())) {
            api = apiRepository.findById(request.getIntegratedApiId())
                    .orElseThrow(() -> new RuntimeException("Integrated API not found: " + request.getIntegratedApiId()));
        }

         IntegrationMapping.MappingType mappingType = IntegrationMapping.MappingType.valueOf(request.getMappingType());

        boolean isDuplicate = mappingRepository.existsByUniqueConstraintExcludingId(
                request.getDynamicRouteId(),
                request.getIntegratedApiId(),
                mappingType,
                request.getData(),
                request.getExternalKey(),
                id
        );

        if (isDuplicate) {
            String errorMsg = String.format(
                    "⚠Duplicate Mapping Detected!\n\n" +
                            "Another mapping with this combination already exists:\n\n" +
                            "• Dynamic Route ID: %s\n" +
                            "• Integrated API ID: %d\n" +
                            "• Mapping Type: %s\n" +
                            "• Data: %s\n" +
                            "• External Key: %s\n\n" +
                            "Cannot update to duplicate values. Please modify one or more fields.",
                    request.getDynamicRouteId(),
                    request.getIntegratedApiId(),
                    request.getMappingType(),
                    request.getData(),
                    request.getExternalKey()
            );

            log.warn("Duplicate mapping on update for ID {}: {}", id, errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        mapRequestToEntity(request, entity, api);

        entity.setUpdatedBy(currentUser);
        entity.setUpdatedAt(LocalDateTime.now());

        IntegrationMapping saved = mappingRepository.save(entity);

        log.info("Updated integration mapping: {} by user: {}", saved.getId(), currentUser);
        return mapEntityToDto(saved);
    }



    @Transactional(readOnly = true)
    public Optional<IntegrationMappingDto> findById(Long id) {
        return mappingRepository.findById(id)
                .map(this::mapEntityToDto);
    }

    @Transactional(readOnly = true)
    public List<IntegrationMappingDto> findByDynamicRoute(String dynamicRouteId) {
        return mappingRepository.findByDynamicRouteIdAndIsActiveTrue(dynamicRouteId)
                .stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<IntegrationMappingDto> findWithAdvancedFilters(
            String dynamicRouteId, Long integratedApiId, String integratedApiCode,
            String mappingType, String data, String externalKey,
            Boolean isActive, String notes,
            LocalDateTime createdAfter, LocalDateTime createdBefore,
            LocalDateTime updatedAfter, LocalDateTime updatedBefore,
            String search, Long minId, Long maxId,
            Pageable pageable) {

        log.debug("Searching with advanced filters - dynamicRouteId: {}, integratedApiId: {}, mappingType: {}",
                dynamicRouteId, integratedApiId, mappingType);

        Specification<IntegrationMapping> spec = IntegrationMappingSpecification.buildSpecification(
                dynamicRouteId, integratedApiId, integratedApiCode, mappingType,
                data, externalKey, isActive, notes,
                createdAfter, createdBefore, updatedAfter, updatedBefore,
                search, minId, maxId
        );

        Page<IntegrationMappingDto> result = mappingRepository.findAll(spec, pageable)
                .map(this::mapEntityToDto);

        log.debug("Found {} results out of {} total", result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    public byte[] exportFile(Specification<IntegrationMapping> spec, String type, String sortedBy, String sortDirection) throws IOException {
        if ("CSV".equalsIgnoreCase(type)) {
            return convertToCSVStreamed(spec, sortedBy, sortDirection);
        } else if ("Excel".equalsIgnoreCase(type) || "XLSX".equalsIgnoreCase(type)) {
            return convertToExcelStreamed(spec, sortedBy, sortDirection);
        } else {
            throw new IllegalArgumentException("Unsupported export type: " + type);
        }
    }
    private byte[] convertToCSVStreamed(Specification<IntegrationMapping> spec, String sortedBy, String sortDirection) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(bos, StandardCharsets.UTF_8))) {
            writer.println("Dynamic Route ID,Integrated API ID,Integrated API Code,Integrated API Name,Mapping Type,Data,External Key,Active,Notes,Created At,Updated At,Created By,Updated By");
            writer.flush();

            int pageSize = 1000;
            int pageNumber = 0;
            boolean hasMore = true;

            while (hasMore) {
                Sort sort = sortDirection.equalsIgnoreCase("asc")
                        ? Sort.by(sortedBy).ascending()
                        : Sort.by(sortedBy).descending();

                Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

                Page<IntegrationMapping> page = mappingRepository.findAll((root, query, criteriaBuilder) -> {
                    root.fetch("integratedApi", JoinType.LEFT);
                    query.distinct(true);
                    return spec != null ? spec.toPredicate(root, query, criteriaBuilder) : null;
                }, pageable);

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                for (IntegrationMapping record : page.getContent()) {
                    writer.append(escapeCsv(record.getDynamicRouteId())).append(",");
                    writer.append(String.valueOf(record.getIntegratedApiId())).append(",");
                    writer.append(escapeCsv(record.getIntegratedApi() != null ? record.getIntegratedApi().getCode() : "")).append(",");
                    writer.append(escapeCsv(record.getIntegratedApi() != null ? record.getIntegratedApi().getName() : "")).append(",");
                    writer.append(record.getMappingType() != null ? record.getMappingType().toString() : "").append(",");
                    writer.append(escapeCsv(record.getData())).append(",");
                    writer.append(escapeCsv(record.getExternalKey())).append(",");
                    writer.append(record.getIsActive() ? "ACTIVE" : "INACTIVE").append(",");
                    writer.append(escapeCsv(record.getNotes())).append(",");
                    writer.append(record.getCreatedAt() != null ? record.getCreatedAt().format(formatter) : "").append(",");
                    writer.append(record.getUpdatedAt() != null ? record.getUpdatedAt().format(formatter) : "").append(",");
                    writer.append(escapeCsv(record.getCreatedBy())).append(",");
                    writer.append(escapeCsv(record.getUpdatedBy())).append("\n");
                }
                writer.flush();

                hasMore = page.hasNext();
                pageNumber++;
            }
            writer.flush();
        }

        return bos.toByteArray();
    }

    private byte[] convertToExcelStreamed(Specification<IntegrationMapping> spec, String sortedBy, String sortDirection) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        Sheet sheet = workbook.createSheet("Integration Mappings");

        try {
            Row header = sheet.createRow(0);
             String[] columns = {
                    "Dynamic Route ID", "Integrated API ID", "Integrated API Code",
                    "Integrated API Name", "Mapping Type", "Data",
                    "External Key", "Active", "Notes",
                    "Created At", "Updated At", "Created By", "Updated By"
            };
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            int[] widths = {20, 15, 20, 25, 20, 30, 20, 10, 35, 20, 20, 25, 25};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            int rowIdx = 1;
            int pageSize = 1000;
            int pageNumber = 0;
            boolean hasMore = true;
            final int MAX_CELL_LENGTH = 20000;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            while (hasMore) {
                Sort sort = sortDirection.equalsIgnoreCase("asc")
                        ? Sort.by(sortedBy).ascending()
                        : Sort.by(sortedBy).descending();

                Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

                Page<IntegrationMapping> page = mappingRepository.findAll((root, query, criteriaBuilder) -> {
                    root.fetch("integratedApi", JoinType.LEFT);
                    query.distinct(true);
                    return spec != null ? spec.toPredicate(root, query, criteriaBuilder) : null;
                }, pageable);

                for (IntegrationMapping record : page.getContent()) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(safeString(record.getDynamicRouteId()));
                    row.createCell(1).setCellValue(record.getIntegratedApiId() != null ? record.getIntegratedApiId() : 0);
                    row.createCell(2).setCellValue(safeString(record.getIntegratedApi() != null ? record.getIntegratedApi().getCode() : ""));
                    row.createCell(3).setCellValue(safeString(record.getIntegratedApi() != null ? record.getIntegratedApi().getName() : ""));
                    row.createCell(4).setCellValue(record.getMappingType() != null ? record.getMappingType().toString() : "");
                    row.createCell(5).setCellValue(safeString(record.getData()));
                    row.createCell(6).setCellValue(safeString(record.getExternalKey()));
                    row.createCell(7).setCellValue(record.getIsActive() ? "ACTIVE" : "INACTIVE");
                    row.createCell(8).setCellValue(truncate(record.getNotes(), MAX_CELL_LENGTH));
                    row.createCell(9).setCellValue(record.getCreatedAt() != null ? record.getCreatedAt().format(formatter) : "");
                    row.createCell(10).setCellValue(record.getUpdatedAt() != null ? record.getUpdatedAt().format(formatter) : "");
                    row.createCell(11).setCellValue(safeString(record.getCreatedBy()));
                    row.createCell(12).setCellValue(safeString(record.getUpdatedBy()));
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

    private String safeString(String value) {
        return value != null ? value : "";
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength - 3) + "...";
    }


    private String convertMappingToCSVRow(IntegrationMapping record) {
        return String.format("%s,%d,%s,%s,%s,%s,%s,%s,%s",
                escapeCsv(record.getDynamicRouteId()),
                record.getIntegratedApiId(),
                escapeCsv(record.getIntegratedApi() != null ? record.getIntegratedApi().getCode() : ""),
                escapeCsv(record.getIntegratedApi() != null ? record.getIntegratedApi().getName() : ""),
                record.getMappingType() != null ? record.getMappingType().toString() : "",
                escapeCsv(record.getData()),
                escapeCsv(record.getExternalKey()),
                record.getIsActive() ? "ACTIVE" : "INACTIVE",
                escapeCsv(record.getNotes())
        );
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }


    @Transactional
    public Map<String, Object> importFromExcel(InputStream inputStream, boolean updateExisting) throws IOException {
        Map<String, Object> result = new HashMap<>();
        List<IntegrationMappingDto> imported = new ArrayList<>();
        List<IntegrationMappingDto> updated = new ArrayList<>();
        List<Map<String, Object>> ignored = new ArrayList<>();
        List<Map<String, Object>> errors = new ArrayList<>();
        int totalRows = 0;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            int rowCount = sheet.getLastRowNum();
            totalRows = rowCount;
            log.info("Processing {} data rows from Excel (updateExisting={}) by user: {}",
                    totalRows, updateExisting, currentUser);

            for (int i = 1; i <= rowCount; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    log.debug("Row {} is null, skipping", i + 1);
                    continue;
                }

                try {
                    IntegrationMappingRequestDto request = parseRowToRequest(row);

                    boolean isEmptyRow = (request.getDynamicRouteId() == null || request.getDynamicRouteId().trim().isEmpty()) &&
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
                        error.put("dynamicRouteId", request.getDynamicRouteId());
                        error.put("integratedApiId", request.getIntegratedApiId());
                        error.put("externalKey", request.getExternalKey());
                        error.put("mappingType", request.getMappingType());
                        error.put("data", request.getData());
                        error.put("errors", validation.get("errors"));
                        errors.add(error);
                        log.warn("Validation failed for row {}: {} - Route: {}, Key: {}",
                                i + 1, validation.get("errors"), request.getDynamicRouteId(), request.getExternalKey());
                        continue;
                    }

                    Optional<IntegrationMapping> existing = mappingRepository
                            .findByDynamicRouteIdAndExternalKey(request.getDynamicRouteId(), request.getExternalKey());

                    if (existing.isPresent() && updateExisting) {
                        IntegrationMapping entity = existing.get();
                        IntegratedApi api = apiRepository.findById(request.getIntegratedApiId())
                                .orElseThrow(() -> new RuntimeException("API not found"));
                        mapRequestToEntity(request, entity, api);

                        entity.setUpdatedBy(currentUser);
                        entity.setUpdatedAt(LocalDateTime.now());

                        IntegrationMapping savedEntity = mappingRepository.save(entity);
                        updated.add(mapEntityToDto(savedEntity));
                        log.debug("Updated existing mapping at row {} by user: {}", i + 1, currentUser);

                    } else if (existing.isPresent()) {
                        Map<String, Object> ignoredItem = new HashMap<>();
                        ignoredItem.put("row", i + 1);
                        ignoredItem.put("externalKey", request.getExternalKey());
                        ignoredItem.put("reason", "Already exists");
                        ignored.add(ignoredItem);

                    } else {
                        IntegratedApi api = apiRepository.findById(request.getIntegratedApiId())
                                .orElseThrow(() -> new RuntimeException("API not found"));
                        IntegrationMapping entity = new IntegrationMapping();
                        mapRequestToEntity(request, entity, api);

                        entity.setCreatedBy(currentUser);
                        entity.setUpdatedBy(currentUser);
                        entity.setCreatedAt(LocalDateTime.now());
                        entity.setUpdatedAt(LocalDateTime.now());

                        IntegrationMapping savedEntity = mappingRepository.save(entity);
                        imported.add(mapEntityToDto(savedEntity));
                        log.debug("Imported new mapping at row {} by user: {}", i + 1, currentUser);
                    }

                } catch (Exception e) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("row", i + 1);
                    error.put("error", e.getMessage());

                    try {
                        IntegrationMappingRequestDto request = parseRowToRequest(row);
                        error.put("dynamicRouteId", request.getDynamicRouteId());
                        error.put("integratedApiId", request.getIntegratedApiId());
                        error.put("externalKey", request.getExternalKey());
                        error.put("mappingType", request.getMappingType());
                        error.put("data", request.getData());
                    } catch (Exception ex) {
                        // If parsing fails, just skip adding row data
                    }

                    errors.add(error);
                    log.error("Error processing row {}: {}", i + 1, e.getMessage());
                }
            }
        }

        result.put("success", errors.isEmpty());
        result.put("totalRows", totalRows);
        result.put("imported", imported.size());
        result.put("updated", updated.size());
        result.put("ignored", ignored.size());
        result.put("errors", errors.size());
        result.put("importedMappings", imported);
        result.put("updatedMappings", updated);
        result.put("ignoredMappings", ignored);
        result.put("errorDetails", errors);

        log.info("Import complete by {}: {} imported, {} updated, {} ignored, {} errors",
                currentUser, imported.size(), updated.size(), ignored.size(), errors.size());

        return result;
    }
    private Map<String, Object> validateMappingForImport(IntegrationMappingRequestDto request) {
        Map<String, Object> validation = new HashMap<>();
        List<String> errors = new ArrayList<>();

        // Validate Dynamic Route ID
        if (request.getDynamicRouteId() == null || request.getDynamicRouteId().trim().isEmpty()) {
            errors.add("Dynamic Route ID is required (Column A cannot be empty)");
        } else if (!dynamicRouteRepository.existsByRouteId(request.getDynamicRouteId())) {
            errors.add("Dynamic Route not found: '" + request.getDynamicRouteId() + "' - Please verify this route exists in your system");
        }

        // Validate Integrated API ID
        if (request.getIntegratedApiId() == null) {
            errors.add("Integrated API ID is required (Column B cannot be empty)");
        } else if (!apiRepository.existsById(request.getIntegratedApiId())) {
            errors.add("Integrated API not found: ID " + request.getIntegratedApiId() + " - Please verify this API exists in your system");
        }

        // Validate External Key
        if (request.getExternalKey() == null || request.getExternalKey().trim().isEmpty()) {
            errors.add("External Key is required (Column G cannot be empty)");
        }

        // Validate Mapping Type
        if (request.getMappingType() == null || request.getMappingType().trim().isEmpty()) {
            errors.add("Mapping Type is required (Column E cannot be empty)");
        } else {
            try {
                IntegrationMapping.MappingType type = IntegrationMapping.MappingType.valueOf(request.getMappingType().toUpperCase().trim());

                if (request.getData() == null || request.getData().trim().isEmpty()) {
                    errors.add("Data is required (Column F cannot be empty)");
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

                        case INDICATOR:
                            // No specific validation for INDICATOR
                            break;

                        default:
                            errors.add("Unknown mapping type: " + type);
                            break;
                    }
                }
            } catch (IllegalArgumentException e) {
                errors.add("Invalid Mapping Type: '" + request.getMappingType() +
                        "'. Valid values are: DATA_ELEMENT, DATA_ELEMENT_WITH_DISAGGREGATION, INDICATOR");
            }
        }

        validation.put("valid", errors.isEmpty());
        validation.put("errors", errors);
        return validation;
    }

    private IntegrationMappingRequestDto parseRowToRequest(Row row) {
        IntegrationMappingRequestDto request = new IntegrationMappingRequestDto();
        request.setDynamicRouteId(getCellValueAsString(row.getCell(0)));
        Double apiId = getCellValueAsDouble(row.getCell(1));
        if (apiId != null) {
            request.setIntegratedApiId(apiId.longValue());
        }
        request.setMappingType(getCellValueAsString(row.getCell(4)));
        request.setData(getCellValueAsString(row.getCell(5)));
        request.setExternalKey(getCellValueAsString(row.getCell(6)));
        String activeStr = getCellValueAsString(row.getCell(7));
        request.setIsActive(activeStr == null || activeStr.equalsIgnoreCase("ACTIVE"));
        request.setNotes(getCellValueAsString(row.getCell(8)));
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

    @Transactional(readOnly = true)
    public List<IntegrationMappingDto> findAll() {
        return mappingRepository.findAll()
                .stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctDynamicRoutes() {
        return mappingRepository.findDistinctDynamicRouteIds();
    }

    public Map<String, Object> validateMapping(IntegrationMappingRequestDto request) {
        Map<String, Object> validation = new HashMap<>();
        List<String> errors = new ArrayList<>();

        if (!dynamicRouteRepository.existsByRouteId(request.getDynamicRouteId())) {
            errors.add("Dynamic Route not found: " + request.getDynamicRouteId());
        }

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

        if (mappingRepository.existsByDynamicRouteIdAndExternalKeyAndIsActiveTrue(
                request.getDynamicRouteId(), request.getExternalKey())) {
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

    private void validateDynamicRouteExists(String dynamicRouteId) {
        if (!dynamicRouteRepository.existsByRouteId(dynamicRouteId)) {
            throw new IllegalArgumentException("Dynamic Route not found: " + dynamicRouteId);
        }
    }
    @Transactional
    public IntegrationMappingDto toggleActiveStatus(Long id) {
        log.info("Toggling active status for integration mapping: {}", id);

        IntegrationMapping entity = mappingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Integration mapping not found: " + id));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        boolean newStatus = !entity.getIsActive();
        entity.setIsActive(newStatus);

        entity.setUpdatedBy(currentUser);
        entity.setUpdatedAt(LocalDateTime.now());

        IntegrationMapping saved = mappingRepository.save(entity);

        log.info("Toggled integration mapping {} status to: {} by user: {}",
                id, newStatus ? "ACTIVE" : "INACTIVE", currentUser);

        return mapEntityToDto(saved);
    }

    @Transactional
    public IntegrationMappingDto activate(Long id) {
        log.info("Activating integration mapping: {}", id);

        IntegrationMapping entity = mappingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Integration mapping not found: " + id));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        entity.setIsActive(true);

        entity.setUpdatedBy(currentUser);
        entity.setUpdatedAt(LocalDateTime.now());

        IntegrationMapping saved = mappingRepository.save(entity);

        log.info("Activated integration mapping: {} by user: {}", id, currentUser);

        return mapEntityToDto(saved);
    }

    @Transactional
    public IntegrationMappingDto deactivate(Long id) {
        log.info("Deactivating integration mapping: {}", id);

        IntegrationMapping entity = mappingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Integration mapping not found: " + id));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        entity.setIsActive(false);

        entity.setUpdatedBy(currentUser);
        entity.setUpdatedAt(LocalDateTime.now());

        IntegrationMapping saved = mappingRepository.save(entity);

        log.info("Deactivated integration mapping: {} by user: {}", id, currentUser);

        return mapEntityToDto(saved);
    }
    private void mapRequestToEntity(IntegrationMappingRequestDto request, IntegrationMapping entity, IntegratedApi api) {
        entity.setDynamicRouteId(request.getDynamicRouteId());
        entity.setIntegratedApi(api);
        entity.setMappingType(IntegrationMapping.MappingType.valueOf(request.getMappingType()));
        entity.setData(request.getData());
        entity.setExternalKey(request.getExternalKey());
        entity.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        entity.setNotes(request.getNotes());
    }

    private IntegrationMappingDto mapEntityToDto(IntegrationMapping entity) {
        return IntegrationMappingDto.builder()
                .id(entity.getId())
                .dynamicRouteId(entity.getDynamicRouteId())
                .integratedApiId(entity.getIntegratedApiId())
                .integratedApiCode(entity.getIntegratedApi() != null ? entity.getIntegratedApi().getCode() : null)
                .integratedApiName(entity.getIntegratedApi() != null ? entity.getIntegratedApi().getName() : null)
                .mappingType(entity.getMappingType().toString())
                .data(entity.getData())
                .externalKey(entity.getExternalKey())
                .isActive(entity.getIsActive())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                 .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }
}