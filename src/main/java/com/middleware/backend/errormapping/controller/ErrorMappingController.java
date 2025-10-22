 package com.middleware.backend.errormapping.controller;

 import com.middleware.backend.dto.RouteOptionDto;
import com.middleware.backend.dto.SourceSystemOptionDto;
 import com.middleware.backend.errormapping.dto.ErrorMappingDto;
 import com.middleware.backend.errormapping.model.ErrorMapping;
 import com.middleware.backend.errormapping.service.BackendErrorMappingService;
 import com.middleware.backend.errormapping.service.ErrorCategoryService;
 import com.middleware.backend.errormapping.spec.ErrorMappingSpecification;
 import com.middleware.backend.service.SourceSystemService;
  import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
 import org.springframework.data.jpa.domain.Specification;
 import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/error-mappings")
@RequiredArgsConstructor
@Slf4j
public class ErrorMappingController {

    private final BackendErrorMappingService errorMappingService;
    private final SourceSystemService sourceSystemService;
    private final ErrorCategoryService errorCategoryService;

    @GetMapping
    @PreAuthorize("hasAuthority('errorMappings:view')")
    @Operation(
            summary = "Get all error mappings",
            description = "Fetches a paginated list of error mappings with optional filters. Supports sorting and pagination. Requires 'errorMappings:view' authority."
    )
    public ResponseEntity<Page<ErrorMappingDto>> getAllErrorMappings(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        try {
            Sort sort = sortDir.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();

            Pageable pageable = PageRequest.of(page, size, sort);
            Page<ErrorMappingDto> result = errorMappingService.getAllErrorMappings(filters, pageable);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error retrieving error mappings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/routes")
    @PreAuthorize("hasAuthority('errorMappings:view')")
    @Operation(
            summary = "Get available routes",
            description = "Retrieves a list of all available routes that can be used for error mappings. Requires 'errorMappings:view' authority."
    )
    public ResponseEntity<List<RouteOptionDto>> getAvailableRoutes() {
        try {
            List<RouteOptionDto> routes = errorMappingService.getAvailableRoutes();
            return ResponseEntity.ok(routes);
        } catch (Exception e) {
            log.error("Error retrieving available routes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/source-systems")
    @PreAuthorize("hasAuthority('sourceSystems:view')")
    @Operation(
            summary = "Get available source systems",
            description = "Retrieves a list of all active source systems. Requires 'errorMappings:view' authority."
    )
    public ResponseEntity<List<SourceSystemOptionDto>> getAvailableSourceSystems() {
        try {
            List<SourceSystemOptionDto> sourceSystems = sourceSystemService.getActiveSourceSystems()
                    .stream()
                    .map(dto -> new SourceSystemOptionDto(dto.getId(), dto.getName(), dto.getDescription(), dto.getActive()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(sourceSystems);
        } catch (Exception e) {
            log.error("Error retrieving available source systems", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // إضافة endpoint جديد للحصول على Error Categories النشطة فقط
    @GetMapping("/error-categories")
    @PreAuthorize("hasAuthority('errorCategories:view')")
    @Operation(
            summary = "Get active error categories",
            description = "Returns a list of active error categories with basic details (id, name, description, status). Requires 'errorMappings:view' authority."
    )
    public ResponseEntity<List<Map<String, Object>>> getActiveErrorCategories() {
        try {
            List<Map<String, Object>> categories = errorCategoryService.getActiveCategories()
                    .stream()
                    .map(dto -> {
                        Map<String, Object> categoryMap = new HashMap<>();
                        categoryMap.put("id", dto.getId());
                        categoryMap.put("name", dto.getName());
                        categoryMap.put("description", dto.getDescription());
                        categoryMap.put("active", dto.getActive());
                        return categoryMap;
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            log.error("Error retrieving active error categories", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('errorMappings:view')")
    @Operation(
            summary = "Get error mapping by ID",
            description = "Retrieves the details of a specific error mapping by its ID. Requires 'errorMappings:view' authority."
    )
    public ResponseEntity<ErrorMappingDto> getErrorMappingById(@PathVariable Long id) {
        try {
            return errorMappingService.getErrorMappingById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error retrieving error mapping with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('errorMappings:create')")
    @Operation(
            summary = "Create error mapping",
            description = "Creates a new error mapping with the provided details. Requires 'errorMappings:create' authority."
    )
    public ResponseEntity<?> createErrorMapping(@RequestBody ErrorMappingDto dto) {
        try {
            ErrorMappingDto created = errorMappingService.createErrorMapping(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid error mapping data: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating error mapping", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to create error mapping"));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('errorMappings:edit')")
    @Operation(
            summary = "Update error mapping",
            description = "Updates an existing error mapping by its ID. Requires 'errorMappings:edit' authority."
    )
    public ResponseEntity<?> updateErrorMapping(@PathVariable Long id, @RequestBody ErrorMappingDto dto) {
        try {
            ErrorMappingDto updated = errorMappingService.updateErrorMapping(id, dto);
            return ResponseEntity.ok(updated);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid error mapping data: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating error mapping with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to update error mapping"));
        }
    }


    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('errorMappings:edit')")
    @Operation(
            summary = "Toggle error mapping status",
            description = "Activates or deactivates an error mapping by flipping its status. Requires 'errorMappings:edit' authority."
    )
    public ResponseEntity<?> toggleErrorMapping(@PathVariable Long id) {
        try {
            errorMappingService.toggleErrorMapping(id);
            return ResponseEntity.ok().body(Map.of("message", "Error mapping status toggled successfully"));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error toggling error mapping with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to toggle error mapping"));
        }
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }



    @GetMapping("/export/{type}")
    @PreAuthorize("hasAuthority('errorMappings:export')")
    @Operation(
            summary = "Export error mappings (CSV/Excel)",
            description = "Exports error mappings into CSV or Excel format with optional filters. Requires 'errorMappings:export' authority."
    )
    public ResponseEntity<byte[]> export(
            @RequestParam Map<String, String> filters,
            @PathVariable("type") String type
    ) {
        try {
            String sortedBy = !filters.get("sortby").equals("")?filters.get("sortby"):"updated_at";
            String sortDir = !filters.get("sortDir").equals("")?filters.get("sortDir"):"desc";
            Specification<ErrorMapping> spec = ErrorMappingSpecification.filter(filters);

            byte[] fileBytes = errorMappingService.exportFile(spec, type, sortedBy, sortDir);

            if (fileBytes == null || fileBytes.length == 0) {
                return ResponseEntity.noContent().build();
            }

            String fileName = "error_mappings." + (type.equalsIgnoreCase("CSV") ? "csv" : "xlsx");
            String contentType = type.equalsIgnoreCase("CSV")
                    ? "text/csv"
                    : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(fileBytes);
        } catch (Exception e) {
            log.error("Error exporting error mappings", e);
            return ResponseEntity.internalServerError().build();
        }
    }

}