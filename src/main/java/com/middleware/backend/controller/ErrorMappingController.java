// تعديل ErrorMappingController.java
package com.middleware.backend.controller;

import com.middleware.backend.dto.ErrorMappingDto;
import com.middleware.backend.dto.RouteOptionDto;
import com.middleware.backend.dto.SourceSystemOptionDto;
import com.middleware.backend.service.ErrorMappingService;
import com.middleware.backend.service.SourceSystemService;
import com.middleware.backend.service.ErrorCategoryService; // إضافة هذا
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/error-mappings")
@RequiredArgsConstructor
@Slf4j
public class ErrorMappingController {

    private final ErrorMappingService errorMappingService;
    private final SourceSystemService sourceSystemService;
    private final ErrorCategoryService errorCategoryService;

    @GetMapping
    @PreAuthorize("hasAuthority('errorMappings:view')")
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

    // باقي methods تبقى كما هي...
    @GetMapping("/match")
    @PreAuthorize("hasAuthority('errorMappings:view')")
    public ResponseEntity<?> findMatchingErrorMapping(
            @RequestParam String routeId,
            @RequestParam(required = false) Long sourceSystemId,
            @RequestParam String errorMessage) {

        try {
            log.info("Smart Error Matching Request - Route: '{}', SourceSystem: {}, Error: '{}'",
                    routeId, sourceSystemId != null ? sourceSystemId : "ANY", errorMessage);

            Optional<ErrorMappingDto> result = errorMappingService.findMatchingErrorMapping(routeId, sourceSystemId, errorMessage);

            if (result.isPresent()) {
                ErrorMappingDto mapping = result.get();
                log.info("Match found - Returning ErrorMapping ID: {}, Code: '{}', Message: '{}'",
                        mapping.getId(), mapping.getMappedErrorCode(), mapping.getMappedMessage());
                return ResponseEntity.ok(result.get());
            } else {
                log.info(" No match found for route: '{}', sourceSystemId: {}, error: '{}'",
                        routeId, sourceSystemId, errorMessage);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            log.error("Error during smart matching for route: '{}', sourceSystemId: {}, error: '{}'",
                    routeId, sourceSystemId, errorMessage, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to find matching error mapping: " + e.getMessage()));
        }
    }

    @GetMapping("/match/debug")
    @PreAuthorize("hasAuthority('errorMappings:view')")
    public ResponseEntity<?> debugErrorMatching(
            @RequestParam String routeId,
            @RequestParam(required = false) Long sourceSystemId,
            @RequestParam String errorMessage) {

        try {
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("routeId", routeId);
            debugInfo.put("sourceSystemId", sourceSystemId);
            debugInfo.put("errorMessage", errorMessage);

            Optional<ErrorMappingDto> result = errorMappingService.findMatchingErrorMapping(routeId, sourceSystemId, errorMessage);

            debugInfo.put("matchFound", result.isPresent());
            if (result.isPresent()) {
                debugInfo.put("matchedMapping", result.get());
            }

            return ResponseEntity.ok(debugInfo);

        } catch (Exception e) {
            log.error("Error in debug matching", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Debug matching failed: " + e.getMessage()));
        }
    }

    @GetMapping("/count/{routeId}")
    @PreAuthorize("hasAuthority('errorMappings:view')")
    public ResponseEntity<Map<String, Long>> getErrorMappingCount(@PathVariable String routeId) {
        try {
            long count = errorMappingService.getCountByRouteId(routeId);
            Map<String, Long> response = new HashMap<>();
            response.put("count", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting error mapping count for route: {}", routeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('errorMappings:export')")
    public ResponseEntity<List<ErrorMappingDto>> exportErrorMappings(
            @RequestParam(required = false) String routeId) {
        try {
            Map<String, String> filters = new HashMap<>();
            if (routeId != null) {
                filters.put("routeId", routeId);
            }

            Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
            Page<ErrorMappingDto> result = errorMappingService.getAllErrorMappings(filters, pageable);

            return ResponseEntity.ok(result.getContent());
        } catch (Exception e) {
            log.error("Error exporting error mappings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('errorMappings:view')")
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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('errorMappings:delete')")
    public ResponseEntity<?> deleteErrorMapping(@PathVariable Long id) {
        try {
            errorMappingService.deleteErrorMapping(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting error mapping with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to delete error mapping"));
        }
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('errorMappings:edit')")
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
    public ResponseEntity<byte[]> export(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @PathVariable("type") String type,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        try {
            Sort sort = sortDir.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();

            Pageable pageable = PageRequest.of(page, size, sort);


            byte[] fileBytes = errorMappingService.exportFile(filters, pageable, type);

            String fileName = "error_configuration." + (type.equalsIgnoreCase("CSV") ? "csv" : "xlsx");
            String contentType = type.equalsIgnoreCase("CSV")
                    ? "text/csv"
                    : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(fileBytes);
        } catch (Exception e) {
            log.error("Error retrieving error mappings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}