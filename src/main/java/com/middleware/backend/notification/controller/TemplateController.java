package com.middleware.backend.notification.controller;

import com.middleware.backend.notification.dto.NotificationTemplateDto;
import com.middleware.backend.notification.enums.ChannelType;
import com.middleware.backend.notification.model.NotificationTemplate;
import com.middleware.backend.notification.service.NotificationTemplateService;
import com.middleware.backend.notification.specification.NotificationTemplateSpecification;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDate;

/**
 * REST Controller for managing notification templates.
 */
@RestController
@RequestMapping("/api/templates")
@AllArgsConstructor
public class TemplateController {

    private final NotificationTemplateService service;

    // ===================== GET TEMPLATE BY ID =====================
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('template:view')")
    @Operation(
            summary = "Get template by ID",
            description = "Retrieve details of a single notification template by its unique ID. Requires 'template:view' authority."
    )
    public ResponseEntity<?> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    // ===================== GET ALL TEMPLATES =====================
    @GetMapping("")
    @PreAuthorize("hasAuthority('template:view')")
    @Operation(
            summary = "List templates with filters",
            description = "Retrieves a paginated list of notification templates. Supports filtering by template name, code, channel type, and creation date range. " +
                    "Supports sorting and pagination. Requires 'template:view' authority."
    )
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "updatedAt") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        Pageable pageable = PageRequest.of(page, size,
                sortDirection.equalsIgnoreCase("asc")
                        ? Sort.by(sortedBy).ascending()
                        : Sort.by(sortedBy).descending());

        ChannelType channelType = null;
        if (type != null) {
            try {
                channelType = ChannelType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        Specification<NotificationTemplate> spec = Specification
                .where(NotificationTemplateSpecification.hasField("name", templateName, NotificationTemplateSpecification.MatchMode.CONTAINS))
                .and(NotificationTemplateSpecification.hasField("code", code, NotificationTemplateSpecification.MatchMode.CONTAINS))
                .and(NotificationTemplateSpecification.hasType(channelType))
                .and(NotificationTemplateSpecification.dateAfter("createdAt", createdAfter))
                .and(NotificationTemplateSpecification.dateBefore("createdAt", createdBefore));

        return ResponseEntity.ok(service.findAll(spec, pageable));
    }

    // ===================== CREATE TEMPLATE =====================
    @PostMapping("")
    @PreAuthorize("hasAuthority('template:create')")
    @Operation(
            summary = "Create a new template",
            description = "Creates a new notification template. Requires 'template:create' authority."
    )
    public ResponseEntity<?> create(@Valid @RequestBody NotificationTemplateDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    // ===================== UPDATE TEMPLATE =====================
    @PutMapping("")
    @PreAuthorize("hasAuthority('template:edit')")
    @Operation(
            summary = "Update an existing template",
            description = "Updates an existing notification template. Requires 'template:edit' authority."
    )
    public ResponseEntity<?> update(@Valid @RequestBody NotificationTemplateDto dto) {
        return ResponseEntity.ok(service.update(dto));
    }

    // ===================== CHANGE STATUS =====================
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('template:edit')")
    @Operation(
            summary = "Toggle template status",
            description = "Toggles the activation status (active/inactive) of a notification template. Requires 'template:edit' authority."
    )
    public ResponseEntity<?> changeStatus(@PathVariable long id) {
        service.changeStatus(id);
        return ResponseEntity.noContent().build();
    }

    // ===================== VALIDATE JSON =====================
    @PostMapping("/validate-json")
    @PreAuthorize("hasAuthority('template:validate')")
    @Operation(
            summary = "Validate JSON structure",
            description = "Validates the structure of a given JSON string for a template body. Requires 'template:validate' authority."
    )
    public ResponseEntity<?> validateJsonStructure(@RequestBody String jsonString) {
        return ResponseEntity.ok(service.validateJsonStructure(jsonString));
    }

    // ===================== GET ALL TEMPLATES FOR DROPDOWNS =====================
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('template:view')")
    @Operation(
            summary = "Retrieve all templates for dropdowns",
            description = "Fetches all templates for dropdown lists or search suggestions. Supports optional search by name or code. Requires 'template:view' authority."
    )
    public ResponseEntity<?> getAllTemplates(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(service.getAllTemplates(search));
    }
}
