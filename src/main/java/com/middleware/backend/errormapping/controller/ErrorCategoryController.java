package com.middleware.backend.errormapping.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.middleware.backend.errormapping.dto.ErrorCategoryDto;
import com.middleware.backend.errormapping.model.ErrorCategory;
import com.middleware.backend.errormapping.service.ErrorCategoryService;
import com.middleware.backend.errormapping.spec.ErrorCategorySpecification;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/error-categories")
@RequiredArgsConstructor
@Slf4j
public class ErrorCategoryController {

    private final ErrorCategoryService categoryService;

    @GetMapping
    @PreAuthorize("hasAuthority('errorCategories:view')")
    public ResponseEntity<Page<?>> getAll(@RequestParam(required = false) String name,
            @RequestParam(required = false) String description, @RequestParam(required = false) String status,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAfter,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdBefore,
            @RequestParam(required = false, defaultValue = "createdAt") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Boolean activeValue = null;
            if ("active".equalsIgnoreCase(status)) {
                activeValue = true;
            } else if ("inactive".equalsIgnoreCase(status)) {
                activeValue = false;
            }

            Specification<ErrorCategory> spec = Specification
                    .where(ErrorCategorySpecification.hasField("name", name,
                            ErrorCategorySpecification.MatchMode.CONTAINS))
                    .and(ErrorCategorySpecification.hasField("description", description,
                            ErrorCategorySpecification.MatchMode.CONTAINS))
                    .and(ErrorCategorySpecification.hasBooleanField("active", activeValue))
                    .and(ErrorCategorySpecification.createdBetween(createdAfter, createdBefore));

            Pageable pageable = PageRequest.of(page, size,
                    sortDirection.equalsIgnoreCase("asc") ? Sort.by(sortedBy).ascending()
                            : Sort.by(sortedBy).descending());

            Page<?> result = categoryService.getAllCategories(spec, pageable);
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('errorCategories:view')")
    public ResponseEntity<?> getCategoryById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(categoryService.getCategoryById(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error retrieving error category with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to retrieve error category"));
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('errorCategories:create')")
    public ResponseEntity<?> createCategory(@RequestBody ErrorCategoryDto dto) {
        try {
            ErrorCategoryDto createdDto = categoryService.createCategory(dto);
            return ResponseEntity.created(URI.create("/api/v1/error-categories/" + createdDto.getId()))
                    .body(createdDto);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid error category data: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating error category", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to create error category"));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('errorCategories:edit')")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody ErrorCategoryDto dto) {
        try {
            return ResponseEntity.ok(categoryService.updateCategory(id, dto));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid error category data: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating error category with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to update error category"));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('errorCategories:delete')")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting error category with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to delete error category"));
        }
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('errorCategories:create')")
    public ResponseEntity<?> toggleCategory(@PathVariable Long id) {
        try {
            ErrorCategoryDto updatedDto = categoryService.toggleCategory(id);
            return ResponseEntity.ok(updatedDto);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error toggling error category with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to toggle error category"));
        }
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }

    @GetMapping("/export/{type}")
    @PreAuthorize("hasAuthority('errorCategories:export')")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String name,
            @RequestParam(required = false) String description, @RequestParam(required = false) String status,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAfter,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdBefore,
            @RequestParam(required = false, defaultValue = "createdAt") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection, @PathVariable("type") String type) {
        try {
            Boolean activeValue = null;
            if ("active".equalsIgnoreCase(status)) {
                activeValue = true;
            } else if ("inactive".equalsIgnoreCase(status)) {
                activeValue = false;
            }

            Specification<ErrorCategory> spec = Specification
                    .where(ErrorCategorySpecification.hasField("name", name,
                            ErrorCategorySpecification.MatchMode.CONTAINS))
                    .and(ErrorCategorySpecification.hasField("description", description,
                            ErrorCategorySpecification.MatchMode.CONTAINS))
                    .and(ErrorCategorySpecification.hasBooleanField("active", activeValue))
                    .and(ErrorCategorySpecification.createdBetween(createdAfter, createdBefore));

            Pageable pageable = PageRequest.of(0, 100000,
                    sortDirection.equalsIgnoreCase("asc") ? Sort.by(sortedBy).ascending()
                            : Sort.by(sortedBy).descending());

            byte[] fileBytes = categoryService.exportFile(spec, pageable, type);

            String fileName = "error_category." + (type.equalsIgnoreCase("CSV") ? "csv" : "xlsx");
            String contentType = type.equalsIgnoreCase("CSV") ? "text/csv"
                    : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

            return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.parseMediaType(contentType)).body(fileBytes);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}