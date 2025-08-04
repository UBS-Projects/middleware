package com.middleware.backend.controller;

import com.middleware.backend.dto.SourceSystemDto;
import com.middleware.backend.service.SourceSystemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityNotFoundException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/source-systems")
@RequiredArgsConstructor
@Slf4j
public class SourceSystemController {

    private final SourceSystemService sourceSystemService;

    @GetMapping
    public ResponseEntity<List<SourceSystemDto>> getAllSourceSystems() {
        try {
            return ResponseEntity.ok(sourceSystemService.getAllSourceSystems());
        } catch (Exception e) {
            log.error("Error retrieving all source systems", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/active")
    public ResponseEntity<List<SourceSystemDto>> getActiveSourceSystems() {
        try {
            return ResponseEntity.ok(sourceSystemService.getActiveSourceSystems());
        } catch (Exception e) {
            log.error("Error retrieving active source systems", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
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
    public ResponseEntity<?> updateSourceSystem(@PathVariable Long id, @RequestBody SourceSystemDto dto) {
        try {
            return ResponseEntity.ok(sourceSystemService.updateSourceSystem(id, dto));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid source system data: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating source system with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to update source system"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSourceSystem(@PathVariable Long id) {
        try {
            sourceSystemService.deleteSourceSystem(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting source system with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to delete source system"));
        }
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<?> toggleSourceSystem(@PathVariable Long id) {
        try {
            SourceSystemDto updatedDto = sourceSystemService.toggleSourceSystem(id);
            return ResponseEntity.ok(updatedDto);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
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
}