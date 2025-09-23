package com.middleware.backend.errormapping.controller;

import com.middleware.backend.dto.ErrorCategoryDto;
import com.middleware.backend.errormapping.model.ErrorCategory;
import com.middleware.backend.errormapping.service.ErrorCategoryService;
import com.middleware.backend.errormapping.spec.ErrorCategorySpecification;
  import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityNotFoundException;
import java.net.URI;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for managing error categories.
 * <p>
 * Exposes endpoints to list, filter, create, update, toggle status, and export
 * error categories. All endpoints are secured with authority-based access
 * checks and documented via OpenAPI annotations.
 */
@RestController
@RequestMapping("/api/error-categories")
@RequiredArgsConstructor
@Slf4j
public class ErrorCategoryController {

    private final ErrorCategoryService categoryService;

    /**
     * Retrieves a paginated list of error categories with optional filters.
     *
     * @param name          optional substring to match against name (contains)
     * @param description   optional substring to match against description (contains)
     * @param status        optional status filter: "active" or "inactive" (case-insensitive)
     * @param createdAfter  optional start date (inclusive) for createdAt filter
     * @param createdBefore optional end date (inclusive) for createdAt filter
     * @param sortedBy      field to sort by (default: createdAt)
     * @param sortDirection sort direction: "asc" or "desc" (default: desc)
     * @param page          zero-based page index
     * @param size          page size
     * @return a paged result containing matching categories
     */
    @GetMapping
    @PreAuthorize("hasAuthority('errorCategories:view')")
    @Operation(
            summary = "Get all error categories",
            description = "Fetches a paginated list of error categories with optional filters (name, description, status, and created date range). Supports sorting and pagination. Requires 'errorCategories:view' authority."
    )
    public ResponseEntity<Page<?>> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String status,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAfter,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdBefore,
            @RequestParam(required = false, defaultValue = "createdAt") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Boolean activeValue = null;
            if ("active".equalsIgnoreCase(status)) {
                activeValue = true;
            } else if ("inactive".equalsIgnoreCase(status)) {
                activeValue = false;
            }

            Specification<ErrorCategory> spec = Specification
                    .where(ErrorCategorySpecification.hasField("name", name, ErrorCategorySpecification.MatchMode.CONTAINS))
                    .and(ErrorCategorySpecification.hasField("description", description, ErrorCategorySpecification.MatchMode.CONTAINS))
                    .and(ErrorCategorySpecification.hasBooleanField("active", activeValue))
                    .and(ErrorCategorySpecification.createdBetween(createdAfter,createdBefore));

            Pageable pageable = PageRequest.of(page, size,
                    sortDirection.equalsIgnoreCase("asc")
                            ? Sort.by(sortedBy).ascending()
                            : Sort.by(sortedBy).descending());

            Page<?> result = categoryService.getAllCategories(spec, pageable);
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }


    /**
     * Retrieves an error category by its id.
     *
     * @param id category identifier
     * @return 200 with the category if found; 404 if not found; 500 on errors
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('errorCategories:view')")
    @Operation(
            summary = "Get error category by ID",
            description = "Retrieves a specific error category using its unique ID. Requires 'errorCategories:view' authority."
    )
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

    /**
     * Creates a new error category.
     *
     * @param dto payload describing the category to create
     * @return 201 with created resource; 400 on validation problems; 500 on errors
     */
    @PostMapping
    @PreAuthorize("hasAuthority('errorCategories:create')")
    @Operation(
            summary = "Create error category",
            description = "Creates a new error category with the provided details. Requires 'errorCategories:create' authority."
    )
    public ResponseEntity<?> createCategory(@RequestBody ErrorCategoryDto dto) {
        try {
            ErrorCategoryDto createdDto = categoryService.createCategory(dto);
            return ResponseEntity.created(URI.create("/api/v1/error-categories/" + createdDto.getId())).body(createdDto);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid error category data: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating error category", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to create error category"));
        }
    }

    /**
     * Updates an existing error category.
     *
     * @param id  the id of the category to update
     * @param dto updated field values
     * @return 200 with updated resource; 404 if not found; 400 on validation/state errors; 500 on errors
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('errorCategories:edit')")
    @Operation(
            summary = "Update error category",
            description = "Updates an existing error category by ID with the provided details. Requires 'errorCategories:edit' authority."
    )
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody ErrorCategoryDto dto) {
        try {
            return ResponseEntity.ok(categoryService.updateCategory(id, dto));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid error category data: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("Cannot deactivate error category: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating error category with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to update error category"));
        }
    }



    /**
     * Toggles the active status of a category.
     *
     * @param id category identifier
     * @return 200 with updated resource; 404 if not found; 400 on invalid state; 500 on errors
     */
    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('errorCategories:edit')")
    @Operation(
            summary = "Toggle error category status",
            description = "Activates or deactivates an error category by flipping its current status. Requires 'errorCategories:edit' authority."
    )
    public ResponseEntity<?> toggleCategory(@PathVariable Long id) {
        try {
            ErrorCategoryDto updatedDto = categoryService.toggleCategory(id);
            return ResponseEntity.ok(updatedDto);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            log.warn("Cannot toggle error category: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error toggling error category with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to toggle error category"));
        }
    }

    /**
     * Utility to wrap an error message for consistent error responses.
     *
     * @param message explanation of the error
     * @return map containing a single entry with key "error"
     */
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }


    /**
     * Exports error categories to CSV or Excel format applying optional filters.
     *
     * @param name          optional substring to match against name
     * @param description   optional substring to match against description
     * @param status        optional status filter: "active" or "inactive"
     * @param createdAfter  optional start date (inclusive)
     * @param createdBefore optional end date (inclusive)
     * @param sortedBy      field to sort by when exporting
     * @param sortDirection sort direction: "asc" or "desc"
     * @param type          file type to export: "CSV" or "EXCEL" (xlsx)
     * @return the generated file as bytes with appropriate content type and filename
     */
    @GetMapping("/export/{type}")
    @PreAuthorize("hasAuthority('errorCategories:export')")
    @Operation(
            summary = "Export error categories",
            description = "Exports error categories in CSV or Excel format with optional filters (name, description, status, and created date range). Requires 'errorCategories:export' authority."
    )
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String status,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAfter,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdBefore,
            @RequestParam(required = false, defaultValue = "createdAt") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @PathVariable("type") String type
    ) {
        try {
            Boolean activeValue = null;
            if ("active".equalsIgnoreCase(status)) {
                activeValue = true;
            } else if ("inactive".equalsIgnoreCase(status)) {
                activeValue = false;
            }

            Specification<ErrorCategory> spec = Specification
                    .where(ErrorCategorySpecification.hasField("name", name, ErrorCategorySpecification.MatchMode.CONTAINS))
                    .and(ErrorCategorySpecification.hasField("description", description, ErrorCategorySpecification.MatchMode.CONTAINS))
                    .and(ErrorCategorySpecification.hasBooleanField("active", activeValue))
                    .and(ErrorCategorySpecification.createdBetween(createdAfter,createdBefore));

            Pageable pageable = PageRequest.of(0, 100000,
                    sortDirection.equalsIgnoreCase("asc")
                            ? Sort.by(sortedBy).ascending()
                            : Sort.by(sortedBy).descending());

            byte[] fileBytes = categoryService.exportFile(spec, pageable, type);

            String fileName = "error_category." + (type.equalsIgnoreCase("CSV") ? "csv" : "xlsx");
            String contentType = type.equalsIgnoreCase("CSV")
                    ? "text/csv"
                    : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(fileBytes);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}