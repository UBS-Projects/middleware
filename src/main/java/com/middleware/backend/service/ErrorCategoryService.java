package com.middleware.backend.service;

import com.middleware.backend.dto.ErrorCategoryDto;
import com.middleware.backend.mapper.ErrorCategoryMapper;
import com.middleware.backend.model.ErrorCategory;
import com.middleware.backend.repository.ErrorCategoryRepository;
import com.middleware.backend.scheduledJobs.model.ScheduledJobs;
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

@Service
@RequiredArgsConstructor
public class ErrorCategoryService {

    private final ErrorCategoryRepository categoryRepository;
    private final ErrorCategoryMapper categoryMapper;

    public Page<ErrorCategory> getAllCategories(Specification<ErrorCategory> spec, Pageable pageable) {
        return categoryRepository.findAll(spec, pageable);
    }

    public List<ErrorCategoryDto> getActiveCategories() {
        return categoryRepository.findByActiveTrue().stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
    }

    public ErrorCategoryDto getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .map(categoryMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + id));
    }

    @Transactional
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

    private String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

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