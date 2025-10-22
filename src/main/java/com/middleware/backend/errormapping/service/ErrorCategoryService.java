package com.middleware.backend.errormapping.service;

import com.middleware.backend.dto.ErrorCategoryDto;
import com.middleware.backend.errormapping.mapper.ErrorCategoryMapper;
import com.middleware.backend.errormapping.model.ErrorCategory;
import com.middleware.backend.errormapping.repository.ErrorCategoryRepository;

 import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
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

import jakarta.persistence.EntityNotFoundException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
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
    public byte[] exportFile(Specification<ErrorCategory> spec, String type, String sortedBy, String sortDir) throws IOException {
        if ("CSV".equalsIgnoreCase(type)) {
            return convertToCSVStreamed(spec, sortedBy, sortDir);
        } else if ("Excel".equalsIgnoreCase(type) || "XLSX".equalsIgnoreCase(type)) {
            return convertToExcelStreamed(spec, sortedBy, sortDir);
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
    private byte[] convertToCSVStreamed(Specification<ErrorCategory> spec, String sortedBy, String sortDir) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(bos, StandardCharsets.UTF_8))) {
            // Write headers
            writer.println("ID,Name,Description,Active,Created At,Updated At");
            writer.flush();

            int pageSize = 1000;
            int pageNumber = 0;
            boolean hasMore = true;

            while (hasMore) {
                Sort sort = sortDir.equalsIgnoreCase("asc")
                        ? Sort.by(sortedBy).ascending()
                        : Sort.by(sortedBy).descending();
                Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
                Page<ErrorCategory> page = categoryRepository.findAll(spec, pageable);

                for (ErrorCategory cat : page.getContent()) {
                    writer.append(String.valueOf(cat.getId())).append(",");
                    writer.append(escapeCsv(cat.getName())).append(",");
                    writer.append(escapeCsv(cat.getDescription())).append(",");
                    writer.append(cat.getActive() != null ? cat.getActive().toString() : "").append(",");
                    writer.append(cat.getCreatedAt() != null ? cat.getCreatedAt().toString() : "").append(",");
                    writer.append(cat.getUpdatedAt() != null ? cat.getUpdatedAt().toString() : "").append("\n");
                }

                writer.flush();
                hasMore = page.hasNext();
                pageNumber++;
            }
            writer.flush();
        }

        return bos.toByteArray();
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

    private String safeString(String value) {
        return value != null ? value : "";
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength - 3) + "...";
    }


    /**
     * Converts a list of categories into an XLSX workbook and returns its bytes.
     *
     * @param categories list to serialize
     * @return xlsx bytes
     * @throws IOException if writing fails
     */
    private byte[] convertToExcelStreamed(Specification<ErrorCategory> spec, String sortedBy, String sortDir) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        Sheet sheet = workbook.createSheet("Error Categories");

        try {
            // Header row
            Row header = sheet.createRow(0);
            String[] columns = { "ID", "Name", "Description", "Active", "Created At", "Updated At" };
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            // Column widths
            int[] widths = { 10, 25, 40, 10, 25, 25 };
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
                Page<ErrorCategory> page = categoryRepository.findAll(spec, pageable);

                for (ErrorCategory cat : page.getContent()) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(cat.getId());
                    row.createCell(1).setCellValue(safeString(cat.getName()));
                    row.createCell(2).setCellValue(truncate(cat.getDescription(), MAX_CELL_LENGTH));
                    row.createCell(3).setCellValue(cat.getActive() != null ? cat.getActive() : false);
                    row.createCell(4).setCellValue(cat.getCreatedAt() != null ? cat.getCreatedAt().toString() : "");
                    row.createCell(5).setCellValue(cat.getUpdatedAt() != null ? cat.getUpdatedAt().toString() : "");
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