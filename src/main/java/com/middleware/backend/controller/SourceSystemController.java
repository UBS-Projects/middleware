package com.middleware.backend.controller;

import com.middleware.backend.dto.SourceSystemDto;
import com.middleware.backend.model.SourceSystem;
import com.middleware.backend.service.SourceSystemService;
import com.middleware.backend.users.specification.SourceSystemSpecification;
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

@RestController
@RequestMapping("/source-systems")
@RequiredArgsConstructor
@Slf4j
public class SourceSystemController {

    private final SourceSystemService sourceSystemService;

    @GetMapping
    @PreAuthorize("hasAuthority('sourceSystems:view')")
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

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('sourceSystems:view')")
    public ResponseEntity<List<SourceSystemDto>> getActiveSourceSystems() {
        try {
            return ResponseEntity.ok(sourceSystemService.getActiveSourceSystems());
        } catch (Exception e) {
            log.error("Error retrieving active source systems", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sourceSystems:view')")
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

    @PostMapping
    @PreAuthorize("hasAuthority('sourceSystems:create')")
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

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('sourceSystems:edit')")
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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('sourceSystems:delete')")
    public ResponseEntity<?> deleteSourceSystem(@PathVariable Long id) {
        try {
            sourceSystemService.deleteSourceSystem(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            log.warn("Cannot delete source system: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting source system with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to delete source system"));
        }
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('sourceSystems:edit')")
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

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }

    @GetMapping("/export/{type}")
    @PreAuthorize("hasAuthority('sourceSystems:export')")
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
                    .and(SourceSystemSpecification.createdBetween(createdAfter,createdBefore));

            Pageable pageable = PageRequest.of(0, 100000,
                    sortDirection.equalsIgnoreCase("asc")
                            ? Sort.by(sortedBy).ascending()
                            : Sort.by(sortedBy).descending());
            byte[] fileBytes = sourceSystemService.exportFile(spec, pageable, type);

            String fileName = "source_systems." + (type.equalsIgnoreCase("CSV") ? "csv" : "xlsx");
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