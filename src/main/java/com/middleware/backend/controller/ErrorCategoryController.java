package com.middleware.backend.controller;

import com.middleware.backend.dto.ErrorCategoryDto;
import com.middleware.backend.model.ErrorCategory;
import com.middleware.backend.service.ErrorCategoryService;
import com.middleware.backend.spec.ErrorCategorySpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/error-categories")
@RequiredArgsConstructor
public class ErrorCategoryController {

    private final ErrorCategoryService categoryService;

    @GetMapping
    public ResponseEntity<Page<?>> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false, defaultValue = "createdAt") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Specification<ErrorCategory> spec = Specification
                    .where(ErrorCategorySpecification.hasField("name", name, ErrorCategorySpecification.MatchMode.CONTAINS))
                    .and(ErrorCategorySpecification.hasField("description", description, ErrorCategorySpecification.MatchMode.CONTAINS));
            Pageable pageable = PageRequest.of(page, size, sortDirection.equalsIgnoreCase("asc")
                    ? Sort.by(sortedBy).ascending()
                    : Sort.by(sortedBy).descending());
            Page<?> result = categoryService.getAllCategories(spec, pageable);
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ErrorCategoryDto> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @PostMapping
    public ResponseEntity<ErrorCategoryDto> createCategory(@RequestBody ErrorCategoryDto dto) {
        ErrorCategoryDto createdDto = categoryService.createCategory(dto);
        return ResponseEntity.created(URI.create("/api/v1/error-categories/" + createdDto.getId())).body(createdDto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ErrorCategoryDto> updateCategory(@PathVariable Long id, @RequestBody ErrorCategoryDto dto) {
        return ResponseEntity.ok(categoryService.updateCategory(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}