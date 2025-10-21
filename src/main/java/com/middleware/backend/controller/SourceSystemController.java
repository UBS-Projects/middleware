package com.middleware.backend.controller;

import com.middleware.backend.dto.SourceSystemDto;
import com.middleware.backend.model.SourceSystem;
import com.middleware.backend.service.SourceSystemService;
import com.middleware.backend.users.specification.SourceSystemSpecification;
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
import java.util.List;
import java.util.Map;

/**
 * REST controller for managing source systems.
 * <p>
 * Provides endpoints to list, filter, create, update, toggle status, get active
 * systems, and export data. Endpoints are protected via authority checks and
 * documented with OpenAPI annotations.
 */
@RestController
@RequestMapping("/api/source-systems")
@RequiredArgsConstructor
@Slf4j
public class SourceSystemController {

    private final SourceSystemService sourceSystemService;

    /**
     * Retrieves a paginated list of source systems with optional filters.
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
     * @return a paged result containing matching source systems
     */
    @GetMapping
    @PreAuthorize("hasAuthority('sourceSystems:view')")
    @Operation(
            summary = "Get all source systems",
            description = "Fetches a paginated list of source systems with optional filters (name, description, status, date range). Supports sorting and pagination. Requires 'sourceSystems:view' authority."
    )
    public ResponseEntity<Page<?>> getAllSourceSystems(
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

            Specification<SourceSystem> spec = Specification
                    .where(SourceSystemSpecification.hasField("name", name, SourceSystemSpecification.MatchMode.CONTAINS))
                    .and(SourceSystemSpecification.hasField("description", description, SourceSystemSpecification.MatchMode.CONTAINS))
                    .and(SourceSystemSpecification.hasBooleanField("active", activeValue))
                    .and(SourceSystemSpecification.createdBetween(createdAfter,createdBefore));

            Pageable pageable = PageRequest.of(page, size,
                    sortDirection.equalsIgnoreCase("asc")
                            ? Sort.by(sortedBy).ascending()
                            : Sort.by(sortedBy).descending());
            return ResponseEntity.ok(sourceSystemService.getAllSourceSystems(spec, pageable));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Retrieves the list of active source systems.
     *
     * @return 200 with list of active systems; 500 on errors
     */
    @GetMapping("/active")
    @PreAuthorize("hasAuthority('sourceSystems:view')")
    @Operation(
            summary = "Get active source systems",
            description = "Retrieves a list of source systems that are currently active. Requires 'sourceSystems:view' authority."
    )
    public ResponseEntity<List<SourceSystemDto>> getActiveSourceSystems() {
        try {
            return ResponseEntity.ok(sourceSystemService.getActiveSourceSystems());
        } catch (Exception e) {
            log.error("Error retrieving active source systems", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Retrieves a source system by id.
     *
     * @param id source system identifier
     * @return 200 with the system if found; 404 if not found; 500 on errors
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sourceSystems:view')")
    @Operation(
            summary = "Get source system by ID",
            description = "Retrieves details of a specific source system by its ID. Requires 'sourceSystems:view' authority."
    )
    public ResponseEntity<?> getSourceSystemById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(sourceSystemService.getSourceSystemById(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error retrieving source system with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to retrieve source system"));
        }
    }

    /**
     * Creates a new source system.
     *
     * @param dto payload with details for the new system
     * @return 201 with created resource; 400 on validation errors; 500 on errors
     */
    @PostMapping
    @PreAuthorize("hasAuthority('sourceSystems:create')")
    @Operation(
            summary = "Create source system",
            description = "Creates a new source system with the provided details. Requires 'sourceSystems:create' authority."
    )
    public ResponseEntity<?> createSourceSystem(@RequestBody SourceSystemDto dto) {
        try {
            SourceSystemDto createdDto = sourceSystemService.createSourceSystem(dto);
            return ResponseEntity.created(URI.create("/api/v1/source-systems/" + createdDto.getId())).body(createdDto);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid source system data: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating source system", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to create source system"));
        }
    }

    /**
     * Updates an existing source system.
     *
     * @param id  the id of the system to update
     * @param dto updated field values
     * @return 200 with updated resource; 404 if not found; 400 on validation/state errors; 500 on errors
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('sourceSystems:edit')")
    @Operation(
            summary = "Update source system",
            description = "Updates an existing source system by its ID. Requires 'sourceSystems:edit' authority."
    )
    public ResponseEntity<?> updateSourceSystem(@PathVariable Long id, @RequestBody SourceSystemDto dto) {
        try {
            return ResponseEntity.ok(sourceSystemService.updateSourceSystem(id, dto));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid source system data: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("Cannot deactivate source system: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating source system with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to update source system"));
        }
    }



    /**
     * Toggles the active status of a source system.
     *
     * @param id source system identifier
     * @return 200 with updated resource; 404 if not found; 400 on invalid state; 500 on errors
     */
    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('sourceSystems:edit')")
    @Operation(
            summary = "Toggle source system status",
            description = "Activates or deactivates a source system by flipping its status. Requires 'sourceSystems:edit' authority."
    )
    public ResponseEntity<?> toggleSourceSystem(@PathVariable Long id) {
        try {
            SourceSystemDto updatedDto = sourceSystemService.toggleSourceSystem(id);
            return ResponseEntity.ok(updatedDto);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            log.warn("Cannot toggle source system: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error toggling source system with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to toggle source system"));
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
     * Exports source systems to CSV or Excel format applying optional filters.
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
    @PreAuthorize("hasAuthority('sourceSystems:export')")
    @Operation(
            summary = "Export source systems (CSV/Excel)",
            description = "Exports source systems into CSV or Excel format with optional filters (name, description, status, date range) and sorting. Requires 'sourceSystems:export' authority."
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

            Specification<SourceSystem> spec = Specification
                    .where(SourceSystemSpecification.hasField("name", name, SourceSystemSpecification.MatchMode.CONTAINS))
                    .and(SourceSystemSpecification.hasField("description", description, SourceSystemSpecification.MatchMode.CONTAINS))
                    .and(SourceSystemSpecification.hasBooleanField("active", activeValue))
                    .and(SourceSystemSpecification.createdBetween(createdAfter, createdBefore));

            byte[] fileBytes = sourceSystemService.exportFile(spec, type, sortedBy, sortDirection);

            if (fileBytes == null || fileBytes.length == 0) {
                return ResponseEntity.noContent().build();
            }

            String fileName = "source_systems." + (type.equalsIgnoreCase("CSV") ? "csv" : "xlsx");
            String contentType = type.equalsIgnoreCase("CSV")
                    ? "text/csv"
                    : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(fileBytes);
        } catch (Exception e) {
            log.error("Error exporting source systems", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}