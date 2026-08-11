package com.middleware.backend.integrated_systems.controller;

import com.middleware.backend.integrated_systems.dto.IntegratedSystemDto;
import com.middleware.backend.integrated_systems.model.IntegratedSystem;
import com.middleware.backend.integrated_systems.service.IntegratedSystemService;
import com.middleware.backend.integrated_systems.spec.IntegratedSystemSpecification;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for managing Integrated Systems.
 * <p>
 * Provides endpoints for listing, retrieving, creating, and updating
 * integrated system configurations.
 */
@RestController
@RequestMapping("/api/integrated-system")
@AllArgsConstructor
public class IntegratedSystemController {

    private final IntegratedSystemService service;

    // ===================== GET ALL INTEGRATED SYSTEMS =====================

    /**
     * Retrieves a paginated list of integrated systems with optional filters.
     *
     * @param code          partial or full system code to filter by
     * @param host          partial host value to filter by
     * @param description   partial description to filter by
     * @param protocol      exact protocol to filter by
     * @param systemType    exact system type to filter by
     * @param createdAfter  include systems created after this date
     * @param createdBefore include systems created before this date
     * @param sortedBy      field name to sort by (default: updatedAt)
     * @param sortDirection sort direction, either asc or desc (default: desc)
     * @param page          zero-based page index
     * @param size          page size
     * @return a paginated {@link Page} of integrated systems
     */
    @GetMapping("")
    @PreAuthorize("hasAuthority('integratedSystem:view')")
    @Operation(
            summary = "List integrated systems with filters",
            description = "Retrieves a paginated list of integrated systems. Supports filtering by code, host, description, " +
                    "protocol, system type, and creation date range. Supports sorting and pagination. Requires 'integratedSystem:view' authority."
    )
    public ResponseEntity<Page<?>> getAll(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String host,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String protocol,
            @RequestParam(required = false) String systemType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdBefore,
            @RequestParam(defaultValue = "updatedAt") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Sort sort = sortDirection.equalsIgnoreCase("asc")
                ? Sort.by(sortedBy).ascending()
                : Sort.by(sortedBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<IntegratedSystem> spec = Specification
                .where(IntegratedSystemSpecification.hasField("code", code, IntegratedSystemSpecification.MatchMode.CONTAINS))
                .and(IntegratedSystemSpecification.hasField("host", host, IntegratedSystemSpecification.MatchMode.CONTAINS))
                .and(IntegratedSystemSpecification.hasField("description", description, IntegratedSystemSpecification.MatchMode.CONTAINS))
                .and(IntegratedSystemSpecification.hasField("protocol", protocol, IntegratedSystemSpecification.MatchMode.EXACT))
                .and(IntegratedSystemSpecification.hasField("systemType", systemType, IntegratedSystemSpecification.MatchMode.EXACT))
                .and(IntegratedSystemSpecification.dateAfter("createdAt", createdAfter))
                .and(IntegratedSystemSpecification.dateBefore("createdAt", createdBefore));

        return ResponseEntity.ok(service.getAll(spec, pageable));
    }

    // ===================== GET INTEGRATED SYSTEM BY CODE =====================
    /**
     * Retrieves a simple list of all integrated system codes.
     * Useful for populating dropdown menus in the frontend.
     *
     * @return list of system codes
     */
    @GetMapping("/codes")
    @PreAuthorize("permitAll()")
    @Operation(
            summary = "Get all system codes for dropdown",
            description = "Retrieves a simple list of all integrated system codes sorted alphabetically. " +
                    "Returns only the codes without any additional details."
    )
    public ResponseEntity<List<String>> getAllCodes() {
        return ResponseEntity.ok(service.getAllCodes());
    }
    /**
     * Retrieves a specific integrated system by its unique code.
     *
     * @param code the unique code identifying the integrated system
     * @return the integrated system details, or 404 if not found
     */
    @GetMapping("/{code}")
    @PreAuthorize("hasAuthority('integratedSystem:view')")
    @Operation(
            summary = "Get integrated system by code",
            description = "Retrieve details of a single integrated system by its unique code. Requires 'integratedSystem:view' authority."
    )
    public ResponseEntity<?> getById(@PathVariable String code) {
        return ResponseEntity.ok(service.getById(code));
    }

    // ===================== CREATE INTEGRATED SYSTEM =====================

    /**
     * Creates a new integrated system.
     *
     * @param body the {@link IntegratedSystemDto} containing system details
     * @return the created integrated system, or 400 if the key already exists
     */
    @PostMapping
    @PreAuthorize("hasAuthority('integratedSystem:create')")
    @Operation(
            summary = "Create a new integrated system",
            description = "Creates a new integrated system. Requires 'integratedSystem:create' authority. Returns 400 if the key already exists."
    )
    public ResponseEntity<?> integrateNewSystem(@RequestBody IntegratedSystemDto body) {
        try {
            IntegratedSystemDto response = service.create(body);
            if (response == null) {
                return ResponseEntity.badRequest().body("Key already exists");
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ===================== UPDATE INTEGRATED SYSTEM =====================

    /**
     * Updates an existing integrated system.
     *
     * @param body the {@link IntegratedSystemDto} containing updated system details
     * @return the updated integrated system, or 400 if not found
     */
    @PutMapping
    @PreAuthorize("hasAuthority('integratedSystem:edit')")
    @Operation(
            summary = "Update an existing integrated system",
            description = "Updates an existing integrated system with the provided details. Requires 'integratedSystem:edit' authority. Returns 400 if the system key is not found."
    )
    public ResponseEntity<?> update(@RequestBody IntegratedSystemDto body) {
        try {
            IntegratedSystemDto response = service.update(body);
            if (response == null) {
                return ResponseEntity.badRequest().body("Key wasn't found");
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
