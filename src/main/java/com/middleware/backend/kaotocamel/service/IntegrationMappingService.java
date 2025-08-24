package com.middleware.backend.kaotocamel.service;

import com.middleware.backend.kaotocamel.dto.IntegrationMappingDto;
import com.middleware.backend.kaotocamel.model.IntegrationMapping;
import com.middleware.backend.kaotocamel.repository.IntegrationMappingRepository;
import com.middleware.backend.kaotocamel.spec.IntegrationMappingSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntegrationMappingService {

    private final IntegrationMappingRepository repository;

    @Transactional
    public IntegrationMappingDto create(IntegrationMappingDto dto) {
        log.info("Creating new integration mapping for API: {}", dto.getApiName());

        IntegrationMapping entity = mapToEntity(dto);
        IntegrationMapping saved = repository.save(entity);

        log.info("Created integration mapping with ID: {}", saved.getId());
        return mapToDto(saved);
    }

    @Transactional
    public IntegrationMappingDto update(Long id, IntegrationMappingDto dto) {
        log.info("Updating integration mapping with ID: {}", id);

        IntegrationMapping entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Integration mapping not found with ID: " + id));

        updateEntityFromDto(entity, dto);
        IntegrationMapping saved = repository.save(entity);

        log.info("Updated integration mapping with ID: {}", saved.getId());
        return mapToDto(saved);
    }

    @Transactional
    public IntegrationMappingDto updateStatus(Long id, boolean isActive) {
        log.info("Updating status for integration mapping with ID: {} to {}", id, isActive);

        IntegrationMapping entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Integration mapping not found with ID: " + id));

        entity.setIsActive(isActive);
        IntegrationMapping saved = repository.save(entity);

        log.info("Updated status for integration mapping with ID: {}", saved.getId());
        return mapToDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting integration mapping with ID: {}", id);

        if (!repository.existsById(id)) {
            throw new RuntimeException("Integration mapping not found with ID: " + id);
        }

        repository.deleteById(id);
        log.info("Deleted integration mapping with ID: {}", id);
    }

    public Optional<IntegrationMappingDto> findById(Long id) {
        log.info("Finding integration mapping by ID: {}", id);

        return repository.findById(id)
                .map(this::mapToDto);
    }

    public Page<IntegrationMappingDto> findAll(Pageable pageable) {
        log.info("Finding all integration mappings with pagination");

        return repository.findAll(pageable)
                .map(this::mapToDto);
    }

    public Page<IntegrationMappingDto> findWithFilters(
            String apiName,
            String externalSystem,
            String datasetId,
            String dataElementId,
            String externalKey,
            Boolean isActive,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore,
            String notes,
            Pageable pageable) {

        log.info("Finding integration mappings with filters - apiName: {}, externalSystem: {}, isActive: {}, notes: {}",
                apiName, externalSystem, isActive, notes != null ? "provided" : "null");

        Specification<IntegrationMapping> spec = buildSpecification(apiName, externalSystem, datasetId,
                dataElementId, externalKey, notes, isActive,
                createdAfter, createdBefore);

        Page<IntegrationMapping> result = repository.findAll(spec, pageable);
        log.info("Found {} filtered results", result.getTotalElements());

        return result.map(this::mapToDto);
    }

    // ===== NEW EXPORT METHODS - FIXED =====

    /**
     * Get all data for export WITHOUT pagination - applies filters correctly
     */
    public List<IntegrationMappingDto> findAllForExport(Sort sort) {
        log.info("Finding ALL integration mappings for export (no filters)");

        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, sort);
        Page<IntegrationMapping> page = repository.findAll(pageable);

        List<IntegrationMappingDto> result = page.getContent()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        log.info("Export: Found {} total records", result.size());
        return result;
    }

    /**
     * Get filtered data for export WITHOUT pagination - applies filters correctly
     */
    public List<IntegrationMappingDto> findAllWithFiltersForExport(
            String apiName,
            String externalSystem,
            String datasetId,
            String dataElementId,
            String externalKey,
            Boolean isActive,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore,
            String notes,
            Sort sort) {

        log.info("Finding FILTERED integration mappings for export");
        log.info("Filters - apiName: {}, externalSystem: {}, isActive: {}, notes: {}",
                apiName, externalSystem, isActive, notes != null ? "provided" : "null");

        Specification<IntegrationMapping> spec = buildSpecification(apiName, externalSystem, datasetId,
                dataElementId, externalKey, notes, isActive,
                createdAfter, createdBefore);

        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, sort);
        Page<IntegrationMapping> page = repository.findAll(spec, pageable);

        List<IntegrationMappingDto> result = page.getContent()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        log.info("Export: Found {} filtered records", result.size());
        return result;
    }

    /**
     * Build JPA Specification for filtering
     */
    private Specification<IntegrationMapping> buildSpecification(
            String apiName, String externalSystem, String datasetId, String dataElementId,
            String externalKey, String notes, Boolean isActive,
            LocalDateTime createdAfter, LocalDateTime createdBefore) {

        return Specification
                .where(IntegrationMappingSpecification.apiNameContains(apiName))
                .and(IntegrationMappingSpecification.externalSystemContains(externalSystem))
                .and(IntegrationMappingSpecification.datasetIdContains(datasetId))
                .and(IntegrationMappingSpecification.dataElementIdContains(dataElementId))
                .and(IntegrationMappingSpecification.externalKeyContains(externalKey))
                .and(IntegrationMappingSpecification.notesContains(notes))
                .and(IntegrationMappingSpecification.hasActiveStatus(isActive))
                .and(IntegrationMappingSpecification.createdAfter(createdAfter))
                .and(IntegrationMappingSpecification.createdBefore(createdBefore));
    }

    // Legacy method for backward compatibility
    public Page<IntegrationMappingDto> findWithFilters(
            String apiName,
            String externalSystem,
            String datasetId,
            String dataElementId,
            String externalKey,
            Boolean isActive,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore,
            Pageable pageable) {

        return findWithFilters(apiName, externalSystem, datasetId, dataElementId,
                externalKey, isActive, createdAfter, createdBefore, null, pageable);
    }

    // ===== SIMPLIFIED EXPORT METHODS =====

    public void exportToExcel(List<IntegrationMappingDto> data, OutputStream outputStream) throws IOException {
        log.info("Generating CLEAN Excel export for {} integration mappings", data.size());

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Integration Mappings");

            // Create styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook, dataStyle);

            int rowNum = 0;

            // Create headers ONLY - NO filter info
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {
                    "ID", "API Name", "External System", "Dataset ID", "Data Element ID",
                    "Category Option Combo ID", "Attribute Option Combo ID", "External Key",
                    "Status", "Created At", "Notes"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Create data rows
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (IntegrationMappingDto mapping : data) {
                Row row = sheet.createRow(rowNum++);
                createExcelDataRow(row, mapping, formatter, dataStyle, dateStyle);
            }

            // Auto-size columns with constraints
            autoSizeColumnsWithConstraints(sheet, headers.length);

            workbook.write(outputStream);
            log.info("CLEAN Excel export completed successfully");
        }
    }

    public void exportToCSV(List<IntegrationMappingDto> data, PrintWriter writer) throws IOException {
        log.info("Generating CLEAN CSV export for {} integration mappings", data.size());

        // Write CSV headers ONLY - NO filter info
        writer.println("ID,API Name,External System,Dataset ID,Data Element ID,Category Option Combo ID,Attribute Option Combo ID,External Key,Status,Created At,Notes");

        // Write data rows
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (IntegrationMappingDto mapping : data) {
            writeCSVDataRow(writer, mapping, formatter);
        }

        writer.flush();
        log.info("CLEAN CSV export completed successfully");
    }

    // ===== EXCEL HELPER METHODS =====

    private void createExcelDataRow(Row row, IntegrationMappingDto mapping, DateTimeFormatter formatter,
                                    CellStyle dataStyle, CellStyle dateStyle) {
        int cellIndex = 0;

        // ID
        Cell idCell = row.createCell(cellIndex++);
        idCell.setCellValue(mapping.getId() != null ? mapping.getId() : 0);
        idCell.setCellStyle(dataStyle);

        // API Name
        Cell apiNameCell = row.createCell(cellIndex++);
        apiNameCell.setCellValue(safeString(mapping.getApiName()));
        apiNameCell.setCellStyle(dataStyle);

        // External System
        Cell externalSystemCell = row.createCell(cellIndex++);
        externalSystemCell.setCellValue(safeString(mapping.getExternalSystem()));
        externalSystemCell.setCellStyle(dataStyle);

        // Dataset ID
        Cell datasetIdCell = row.createCell(cellIndex++);
        datasetIdCell.setCellValue(safeString(mapping.getDatasetId()));
        datasetIdCell.setCellStyle(dataStyle);

        // Data Element ID
        Cell dataElementIdCell = row.createCell(cellIndex++);
        dataElementIdCell.setCellValue(safeString(mapping.getDataElementId()));
        dataElementIdCell.setCellStyle(dataStyle);

        // Category Option Combo ID
        Cell categoryOptionComboIdCell = row.createCell(cellIndex++);
        categoryOptionComboIdCell.setCellValue(safeString(mapping.getCategoryOptionComboId()));
        categoryOptionComboIdCell.setCellStyle(dataStyle);

        // Attribute Option Combo ID
        Cell attributeOptionComboIdCell = row.createCell(cellIndex++);
        attributeOptionComboIdCell.setCellValue(safeString(mapping.getAttributeOptionComboId()));
        attributeOptionComboIdCell.setCellStyle(dataStyle);

        // External Key
        Cell externalKeyCell = row.createCell(cellIndex++);
        externalKeyCell.setCellValue(safeString(mapping.getExternalKey()));
        externalKeyCell.setCellStyle(dataStyle);

        // Status
        Cell statusCell = row.createCell(cellIndex++);
        statusCell.setCellValue(mapping.getIsActive() != null && mapping.getIsActive() ? "ACTIVE" : "INACTIVE");
        statusCell.setCellStyle(dataStyle);

        // Created At
        Cell createdAtCell = row.createCell(cellIndex++);
        createdAtCell.setCellValue(mapping.getCreatedAt() != null ? mapping.getCreatedAt().format(formatter) : "");
        createdAtCell.setCellStyle(dateStyle);

        // Notes
        Cell notesCell = row.createCell(cellIndex);
        notesCell.setCellValue(safeString(mapping.getNotes()));
        notesCell.setCellStyle(dataStyle);
    }

    private void writeCSVDataRow(PrintWriter writer, IntegrationMappingDto mapping, DateTimeFormatter formatter) {
        StringBuilder line = new StringBuilder();

        line.append(escapeCsvValue(mapping.getId() != null ? mapping.getId().toString() : "")).append(",");
        line.append(escapeCsvValue(safeString(mapping.getApiName()))).append(",");
        line.append(escapeCsvValue(safeString(mapping.getExternalSystem()))).append(",");
        line.append(escapeCsvValue(safeString(mapping.getDatasetId()))).append(",");
        line.append(escapeCsvValue(safeString(mapping.getDataElementId()))).append(",");
        line.append(escapeCsvValue(safeString(mapping.getCategoryOptionComboId()))).append(",");
        line.append(escapeCsvValue(safeString(mapping.getAttributeOptionComboId()))).append(",");
        line.append(escapeCsvValue(safeString(mapping.getExternalKey()))).append(",");
        line.append(escapeCsvValue(mapping.getIsActive() != null && mapping.getIsActive() ? "ACTIVE" : "INACTIVE")).append(",");
        line.append(escapeCsvValue(mapping.getCreatedAt() != null ? mapping.getCreatedAt().format(formatter) : "")).append(",");
        line.append(escapeCsvValue(safeString(mapping.getNotes())));

        writer.println(line.toString());
    }

    private void autoSizeColumnsWithConstraints(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);

            // Set minimum width
            if (currentWidth < 2000) {
                sheet.setColumnWidth(i, 2000);
            }
            // Set maximum width to prevent too wide columns
            if (currentWidth > 8000) {
                sheet.setColumnWidth(i, 8000);
            }
        }
    }

    // ===== STYLE HELPER METHODS =====

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        return headerStyle;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        return dataStyle;
    }

    private CellStyle createDateStyle(Workbook workbook, CellStyle baseStyle) {
        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.cloneStyleFrom(baseStyle);
        CreationHelper createHelper = workbook.getCreationHelper();
        dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
        return dateStyle;
    }

    // ===== UTILITY METHODS =====

    private String safeString(String value) {
        return value != null ? value : "";
    }

    private String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }

        // If value contains comma, quotes, or newlines, wrap in quotes and escape internal quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }

    // ===== MAPPING METHODS =====

    private IntegrationMapping mapToEntity(IntegrationMappingDto dto) {
        IntegrationMapping entity = new IntegrationMapping();
        entity.setApiName(dto.getApiName());
        entity.setExternalSystem(dto.getExternalSystem());
        entity.setDatasetId(dto.getDatasetId());
        entity.setDataElementId(dto.getDataElementId());
        entity.setCategoryOptionComboId(dto.getCategoryOptionComboId());
        entity.setAttributeOptionComboId(dto.getAttributeOptionComboId());
        entity.setExternalKey(dto.getExternalKey());
        entity.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        entity.setNotes(dto.getNotes());
        return entity;
    }

    private IntegrationMappingDto mapToDto(IntegrationMapping entity) {
        IntegrationMappingDto dto = new IntegrationMappingDto();
        dto.setId(entity.getId());
        dto.setApiName(entity.getApiName());
        dto.setExternalSystem(entity.getExternalSystem());
        dto.setDatasetId(entity.getDatasetId());
        dto.setDataElementId(entity.getDataElementId());
        dto.setCategoryOptionComboId(entity.getCategoryOptionComboId());
        dto.setAttributeOptionComboId(entity.getAttributeOptionComboId());
        dto.setExternalKey(entity.getExternalKey());
        dto.setIsActive(entity.getIsActive());
        dto.setNotes(entity.getNotes());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    private void updateEntityFromDto(IntegrationMapping entity, IntegrationMappingDto dto) {
        entity.setApiName(dto.getApiName());
        entity.setExternalSystem(dto.getExternalSystem());
        entity.setDatasetId(dto.getDatasetId());
        entity.setDataElementId(dto.getDataElementId());
        entity.setCategoryOptionComboId(dto.getCategoryOptionComboId());
        entity.setAttributeOptionComboId(dto.getAttributeOptionComboId());
        entity.setExternalKey(dto.getExternalKey());
        entity.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : entity.getIsActive());
        entity.setNotes(dto.getNotes());
    }
}