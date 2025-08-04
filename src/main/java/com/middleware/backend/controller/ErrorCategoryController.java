package com.middleware.backend.controller;

import com.middleware.backend.dto.ErrorCategoryDto;
import com.middleware.backend.service.ErrorCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/error-categories")
@RequiredArgsConstructor
public class ErrorCategoryController {

    private final ErrorCategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<ErrorCategoryDto>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
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