package com.middleware.backend.service;

import com.middleware.backend.dto.ErrorCategoryDto;
import com.middleware.backend.mapper.ErrorCategoryMapper;
import com.middleware.backend.model.ErrorCategory;
import com.middleware.backend.repository.ErrorCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ErrorCategoryService {

    private final ErrorCategoryRepository categoryRepository;
    private final ErrorCategoryMapper categoryMapper;

    public List<ErrorCategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream()
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
        ErrorCategory category = categoryMapper.toEntity(dto);
        ErrorCategory savedCategory = categoryRepository.save(category);
        return categoryMapper.toDto(savedCategory);
    }

    @Transactional
    public ErrorCategoryDto updateCategory(Long id, ErrorCategoryDto dto) {
        ErrorCategory existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + id));

        existingCategory.setName(dto.getName());
        existingCategory.setDescription(dto.getDescription());

        ErrorCategory updatedCategory = categoryRepository.save(existingCategory);
        return categoryMapper.toDto(updatedCategory);
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new EntityNotFoundException("Category not found with id: " + id);
        }
         long usageCount = categoryRepository.countErrorMappingsByCategoryId(id);
        if (usageCount > 0) {
            throw new IllegalStateException("Cannot delete category with id " + id + " because it is used by " + usageCount + " error mapping(s).");
        }
        categoryRepository.deleteById(id);
    }
}