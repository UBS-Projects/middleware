package com.middleware.backend.errormapping.service;

import com.middleware.backend.dto.ErrorCategoryDto;
import com.middleware.backend.errormapping.mapper.ErrorCategoryMapper;
import com.middleware.backend.errormapping.model.ErrorCategory;
import com.middleware.backend.errormapping.repository.ErrorCategoryRepository;

 import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for managing error categories.
 * <p>
 * Encapsulates validation rules (e.g., unique name, prevent deactivation when in use),
 * mapping between DTOs and entities, audit field updates based on the current user,
 * and export to CSV/Excel formats.
 */
@Service
@RequiredArgsConstructor
public class ErrorCategoryService {

    private final ErrorCategoryRepository categoryRepository;
    private final ErrorCategoryMapper categoryMapper;

    /**
     * Retrieves a page of categories matching the given specification.
     *
     * @param spec     specification with optional filters
     * @param pageable pagination and sorting options
     * @return page of matching categories
     */
    public Page<ErrorCategory> getAllCategories(Specification<ErrorCategory> spec, Pageable pageable) {
        return categoryRepository.findAll(spec, pageable);
    }

    /**
     * Returns all active categories as DTOs.
     *
     * @return list of active category DTOs
     */
    public List<ErrorCategoryDto> getActiveCategories() {
        return categoryRepository.findByActiveTrue().stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a single category by id.
     *
     * @param id category identifier
     * @return the mapped DTO
     * @throws jakarta.persistence.EntityNotFoundException when not found
     */
    public ErrorCategoryDto getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .map(categoryMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + id));
    }

    @Transactional
    /**
     * Creates a new error category after enforcing uniqueness constraint.
     * Sets audit fields using the current authenticated user.
     *
     * @param dto input DTO
     * @return created category as DTO
     * @throws IllegalArgumentException when name already exists
     */
    public ErrorCategoryDto createCategory(ErrorCategoryDto dto) {
        if (categoryRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("Category with name '" + dto.getName() + "' already exists.");
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUser = authentication.getName();
        dto.setCreatedBy(emailUser);
        dto.setUpdatedBy(emailUser);
        ErrorCategory category = categoryMapper.toEntity(dto);
        ErrorCategory savedCategory = categoryRepository.save(category);
        return categoryMapper.toDto(savedCategory);
    }

    @Transactional
    /**
     * Updates an existing category, validating name uniqueness and preventing
     * deactivation when the category is referenced by error mappings.
     * Updates audit fields using the current authenticated user.
     *
     * @param id  category id
     * @param dto updated values
     * @return updated category as DTO
     * @throws jakarta.persistence.EntityNotFoundException when the category does not exist
     * @throws IllegalArgumentException when renaming to an existing name
     * @throws IllegalStateException when attempting to deactivate a used category
     */
    public ErrorCategoryDto updateCategory(Long id, ErrorCategoryDto dto) {
        ErrorCategory existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + id));

        // Check if name is changing and if new name already exists
        if (!existingCategory.getName().equalsIgnoreCase(dto.getName()) &&
                categoryRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("Category with name '" + dto.getName() + "' already exists.");
        }

        // Check if trying to deactivate a category that is currently active and has error mappings
        if (existingCategory.getActive() && !dto.getActive()) {
            long usageCount = categoryRepository.countErrorMappingsByCategoryId(id);
            if (usageCount > 0) {
                throw new IllegalStateException("Cannot deactivate category with id " + id + " because it is used by " + usageCount + " error mapping(s).");
            }
        }

        existingCategory.setName(dto.getName());
        existingCategory.setDescription(dto.getDescription());
        existingCategory.setActive(dto.getActive());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUser = authentication.getName();
        existingCategory.setUpdatedBy(emailUser);
        existingCategory.setUpdatedAt(LocalDateTime.now());
        ErrorCategory updatedCategory = categoryRepository.save(existingCategory);
        return categoryMapper.toDto(updatedCategory);
    }


    @Transactional
    /**
     * Toggles the active status of a category, ensuring active-to-inactive
     * transitions are allowed only when not referenced by error mappings.
     * Updates audit fields using the current authenticated user.
     *
     * @param id category id
     * @return updated category as DTO
     * @throws jakarta.persistence.EntityNotFoundException when the category does not exist
     * @throws IllegalStateException when attempting to deactivate a used category
     */
    public ErrorCategoryDto toggleCategory(Long id) {
        ErrorCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + id));

        // If trying to deactivate an active category, check for error mappings
        if (category.getActive()) {
            long usageCount = categoryRepository.countErrorMappingsByCategoryId(id);
            if (usageCount > 0) {
                throw new IllegalStateException("Cannot deactivate category with id " + id + " because it is used by " + usageCount + " error mapping(s).");
            }
        }

        category.setActive(!category.getActive());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUser = authentication.getName();
        category.setUpdatedBy(emailUser);
        category.setUpdatedAt(LocalDateTime.now());
        ErrorCategory savedCategory = categoryRepository.save(category);
        return categoryMapper.toDto(savedCategory);
    }

    /**
     * Exports categories matching the specification into CSV or XLSX format.
     *
     * @param spec     filters to apply
     * @param pageable sort/pagination used to bound and order the export
     * @param type     export type: "CSV", "Excel", or "XLSX"
     * @return file bytes
     * @throws IllegalArgumentException when an unsupported type is requested
     */
    public byte[] exportFile(Specification<ErrorCategory> spec, Pageable pageable, String type) {
        Page<ErrorCategory> res = categoryRepository.findAll(spec, pageable);
        List<ErrorCategory> data = res.getContent();

        if ("CSV".equalsIgnoreCase(type)) {
            return convertToCSV(data).getBytes(StandardCharsets.UTF_8);
        } else if ("Excel".equalsIgnoreCase(type) || "XLSX".equalsIgnoreCase(type)) {
            try {
                return convertToExcel(data);
            } catch (IOException e) {
                throw new RuntimeException("Error generating Excel file", e);
            }
        } else {
            throw new IllegalArgumentException("Unsupported export type: " + type);
        }
    }

    /**
     * Converts a list of categories into a CSV string.
     *
     * @param categories list to serialize
     * @return CSV contents
     */
    private String convertToCSV(List<ErrorCategory> categories) {
        StringBuilder sb = new StringBuilder();
        // CSV headers
        sb.append("ID,Name,Description,Active,CreatedAt,UpdatedAt\n");

        for (ErrorCategory cat : categories) {
            sb.append(cat.getId()).append(",");
            sb.append(escapeCsv(cat.getName())).append(",");
            sb.append(escapeCsv(cat.getDescription())).append(",");
            sb.append(cat.getActive()).append(",");
            sb.append(cat.getCreatedAt() != null ? cat.getCreatedAt().toString() : "").append(",");
            sb.append(cat.getUpdatedAt() != null ? cat.getUpdatedAt().toString() : "").append("\n");
        }
        return sb.toString();
    }

    /**
     * Escapes a CSV field by doubling quotes and quoting when necessary.
     *
     * @param value raw field value
     * @return escaped CSV field
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    /**
     * Converts a list of categories into an XLSX workbook and returns its bytes.
     *
     * @param categories list to serialize
     * @return xlsx bytes
     * @throws IOException if writing fails
     */
    private byte[] convertToExcel(List<ErrorCategory> categories) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Error Categories");

            // Header row
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("Name");
            header.createCell(2).setCellValue("Description");
            header.createCell(3).setCellValue("Active");
            header.createCell(4).setCellValue("CreatedAt");
            header.createCell(5).setCellValue("UpdatedAt");

            // Data rows
            int rowIdx = 1;
            for (ErrorCategory cat : categories) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(cat.getId());
                row.createCell(1).setCellValue(cat.getName());
                row.createCell(2).setCellValue(cat.getDescription());
                row.createCell(3).setCellValue(cat.getActive() != null && cat.getActive());
                row.createCell(4).setCellValue(cat.getCreatedAt() != null ? cat.getCreatedAt().toString() : "");
                row.createCell(5).setCellValue(cat.getUpdatedAt() != null ? cat.getUpdatedAt().toString() : "");
            }

            // Autosize columns
            for (int i = 0; i <= 5; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to byte array
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                workbook.write(bos);
                return bos.toByteArray();
            }
        }
    }
}