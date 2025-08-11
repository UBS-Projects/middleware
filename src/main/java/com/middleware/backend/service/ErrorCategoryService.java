package com.middleware.backend.service;

import com.middleware.backend.dto.ErrorCategoryDto;
import com.middleware.backend.mapper.ErrorCategoryMapper;
import com.middleware.backend.model.ErrorCategory;
import com.middleware.backend.repository.ErrorCategoryRepository;
import com.middleware.backend.scheduledJobs.model.ScheduledJobs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

        existingCategory.setName(dto.getName());
        existingCategory.setDescription(dto.getDescription());
        existingCategory.setActive(dto.getActive());

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

    @Transactional
    public ErrorCategoryDto toggleCategory(Long id) {
        ErrorCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + id));

        category.setActive(!category.getActive());
        ErrorCategory savedCategory = categoryRepository.save(category);
        return categoryMapper.toDto(savedCategory);
    }
}
